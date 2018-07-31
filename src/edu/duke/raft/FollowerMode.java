package edu.duke.raft;
import java.util.Random;
import java.util.Timer;
import java.lang.*;

public class FollowerMode extends RaftMode {
  //new3
  Timer hbtim;
  public void go () {
    synchronized (mLock) {
      int term = mConfig.getCurrentTerm();

      System.out.println ("S" +
			  mID +
			  "." +
			  term +
			  ": switched to follower mode.");
        Random rn = new Random();
        //random.nextInt(max-min+1)+min
        int time = rn.nextInt(ELECTION_TIMEOUT_MAX-ELECTION_TIMEOUT_MIN+1) +  ELECTION_TIMEOUT_MIN;
         hbtim = scheduleTimer(Long.valueOf(time), 0);
    }
  }

  // @param candidate’s term
  // @param candidate requesting vote
  // @param index of candidate’s last log entry
  // @param term of candidate’s last log entry
  // @return 0, if server votes for candidate; otherwise, server's
  // current term
  public int requestVote (int candidateTerm,
			  int candidateID,
			  int lastLogIndex,
			  int lastLogTerm) {
    synchronized (mLock) {

      int term = mConfig.getCurrentTerm ();
      int vote = term;
      if(term>candidateTerm){
        return term;
      }
      if(mConfig.getVotedFor() == 0||mConfig.getVotedFor()==candidateID){
        //make sure candidates log is as up to date as receivers log #2
        //Request vote RPC #2
        if(lastLogTerm < mLog.getLastTerm()){
          
          return term;
        }
        if(lastLogTerm==mLog.getLastTerm()&&lastLogIndex<mLog.getLastIndex()){
          return term;
        }
        if(mCommitIndex!=0){
          if(lastLogIndex!=mLog.getLastIndex() || lastLogTerm!=mLog.getLastTerm()){
            return term;
          }
        }
      //  System.out.println("candidate requesting vote/ term"+ candidateID + " "+
        //candidateTerm + "my id /term"+ mID+ mConfig.getCurrentTerm());
        hbtim.cancel();
        mConfig.setCurrentTerm(candidateTerm,candidateID);
        Random rn = new Random();
        //random.nextInt(max-min+1)+min
        int time = rn.nextInt(ELECTION_TIMEOUT_MAX-ELECTION_TIMEOUT_MIN+1) +  ELECTION_TIMEOUT_MIN;

        hbtim = scheduleTimer(Long.valueOf(time), 0);
        return 0;
      }
      return vote;
    }
  }


  // @param leader’s term
  // @param current leader
  // @param index of log entry before entries to append
  // @param term of log entry before entries to append
  // @param entries to append (in order of 0 to append.length-1)
  // @param index of highest committed entry
  // @return 0, if server appended entries; otherwise, server's
  // current term
  public int appendEntries (int leaderTerm,
			    int leaderID,
			    int prevLogIndex,
			    int prevLogTerm,
			    Entry[] entries,
			    int leaderCommit) {
    synchronized (mLock) {
      Random rn = new Random();
      //random.nextInt(max-min+1)+min
      int time = rn.nextInt(ELECTION_TIMEOUT_MAX-ELECTION_TIMEOUT_MIN+1) +  ELECTION_TIMEOUT_MIN;

      int term = mConfig.getCurrentTerm ();
      int result = term;
      if(leaderTerm>term){
        mConfig.setCurrentTerm(leaderTerm, 0);
        RaftResponses.setTerm(leaderTerm);
        RaftResponses.clearAppendResponses(leaderTerm);
        RaftResponses.clearVotes(leaderTerm);
        hbtim.cancel();
        hbtim = scheduleTimer(Long.valueOf(time), 0);
        if(entries!=null){
          mCommitIndex = 1;
          System.out.println(mID + " appended entries from "+ leaderID);
          mLog.insert(entries,-1,0);
        }

        return 0;
      }
      else if(leaderTerm==term){
        hbtim.cancel();
        hbtim = scheduleTimer(Long.valueOf(time), 0);
        if(entries!=null){
          System.out.println(mID + " appended entries from "+ leaderID);

          mLog.insert(entries,-1,0);
          mCommitIndex = 1;

        }

        return 0;

      }
      else{
        return term;
      }

    }
  }

  // @param id of the timer that timed out
  public void handleTimeout (int timerID) {
    synchronized (mLock) {
      RaftServerImpl.setMode(new CandidateMode());
    }
  }
}
