package edu.duke.raft;
import java.util.Random;
import java.util.Timer;

public class LeaderMode extends RaftMode {
  //new3
  Timer hb_timer;
  Entry[] entries;
  public void go () {
    synchronized (mLock) {
      int term = mConfig.getCurrentTerm();

      System.out.println ("S" +
			  mID +
			  "." +
			  term +
			  ": switched to leader mode.");
    entries= new Entry[mLog.getLastIndex()+1];
    for(int i =0;i<=mLog.getLastIndex();i++){
      entries[i] = mLog.getEntry(i);

    }
    mCommitIndex =1;
    //RaftResponses.clearAppendResponses(mConfig.getCurrentTerm());
    for(int i=1; i<= mConfig.getNumServers(); i++){
      remoteAppendEntries(i, mConfig.getCurrentTerm(), mID, mLog.getLastIndex(), mLog.getLastTerm(), null, mCommitIndex);
    }
    hb_timer = scheduleTimer(Long.valueOf(HEARTBEAT_INTERVAL), 1);
    for(int i=1; i<=mConfig.getNumServers(); i++){
      remoteAppendEntries(i, mConfig.getCurrentTerm(), mID, mLog.getLastIndex(), mLog.getLastTerm(), entries, mCommitIndex);
    }

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

      if(candidateTerm> mConfig.getCurrentTerm()){
      //   if(lastLogTerm < mLog.getLastTerm()){
      //     return term;
      //   }
      //   if(lastLogTerm==mLog.getLastTerm()&&lastLogIndex<mLog.getLastIndex()){
      //     return term;
      //   }
        hb_timer.cancel();
        mConfig.setCurrentTerm(candidateTerm, 0);

        RaftServerImpl.setMode(new FollowerMode());
      }
      int vote = term;
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
      int term = mConfig.getCurrentTerm ();
      int result = term;
      if(leaderTerm> term){
        mConfig.setCurrentTerm(leaderTerm,0);
        hb_timer.cancel();
        if(entries!=null){
          mCommitIndex = 1;
          System.out.println(mID + " appended entries from "+ leaderID);

          mLog.insert(entries, -1, 0);
        }
        RaftServerImpl.setMode(new FollowerMode());
        return 0;

      }
      return result;
    }
  }

  // @param id of the timer that timed out
  public void handleTimeout (int timerID) {
    synchronized (mLock) {
      //Entry[] entries= new Entry[mLog.getLastIndex()+1];
      //for(int i =0;i<=mLog.getLastIndex();i++){
        //entries[i] = mLog.getEntry(i);

      //}
      int[] votes;
      votes = RaftResponses.getAppendResponses(mConfig.getCurrentTerm());

      for(int i=1; i<= mConfig.getNumServers(); i++){
        if(votes!=null){
        if(votes[i]!=0){
          remoteAppendEntries(i, mConfig.getCurrentTerm(), mID, mLog.getLastIndex(), mLog.getLastTerm() ,entries, mCommitIndex);

        }
        else{
          remoteAppendEntries(i, mConfig.getCurrentTerm(), mID, mLog.getLastIndex(), mLog.getLastTerm(), null, mCommitIndex);
}
      }
        else{
          remoteAppendEntries(i, mConfig.getCurrentTerm(), mID, mLog.getLastIndex(), mLog.getLastTerm(), null, mCommitIndex);
}
      }
      hb_timer = scheduleTimer(Long.valueOf(HEARTBEAT_INTERVAL), 1);
    }


    }
  }
