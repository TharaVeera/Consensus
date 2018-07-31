package edu.duke.raft;
import java.util.Timer;
import java.util.Random;

public class CandidateMode extends RaftMode {
  //new3
  Timer hb_timer;
  Timer elec_tim;
  public void go () {
    synchronized (mLock) {

      int term = mConfig.getCurrentTerm() + 1;
      mConfig.setCurrentTerm(term, mID);
      System.out.println ("S" +
			  mID +
			  "." +
			  term +
			  ": switched to candidate mode.");
    RaftResponses.setTerm(term);
    RaftResponses.clearVotes(term);
    RaftResponses.clearAppendResponses(term);

    Random rn = new Random();
    //random.nextInt(max-min+1)+min
    int time = rn.nextInt(ELECTION_TIMEOUT_MAX-ELECTION_TIMEOUT_MIN+1) +  ELECTION_TIMEOUT_MIN;

    elec_tim = scheduleTimer(Long.valueOf(time), 0);
    hb_timer = scheduleTimer(Long.valueOf(75), 1);
    for(int i=1; i<= mConfig.getNumServers(); i++){
      remoteRequestVote(i, term, mID, mLog.getLastIndex(), mLog.getLastTerm());
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

      if(candidateID==mID){
        return 0;
      }
      else{
        if(candidateTerm > term){
          if(lastLogIndex<mLog.getLastIndex()|| lastLogTerm < mLog.getLastTerm()){
            return term;
          }
          if(mCommitIndex!=0){
            if(lastLogIndex!=mLog.getLastIndex() || lastLogTerm!=mLog.getLastTerm()){
              return term;
            }
          }
          hb_timer.cancel();
          elec_tim.cancel();

          //System.out.println("candidate requesting vote/ term"+ candidateID + " "+
          //candidateTerm + "my id /term"+ mID+ mConfig.getCurrentTerm());
          mConfig.setCurrentTerm(candidateTerm, candidateID);

          RaftServerImpl.setMode(new FollowerMode());
          return 0;
        }
        else{
          return term;
        }
      }
      //gotta do some timer shit where


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
      if(leaderTerm>=term){
        RaftResponses.setTerm(leaderTerm);
        RaftResponses.clearVotes(leaderTerm);
        RaftResponses.clearAppendResponses(leaderTerm);
        hb_timer.cancel();
        elec_tim.cancel();
        mConfig.setCurrentTerm(leaderTerm, 0);
        if(entries!=null){

          System.out.println(mID + " appended entries from "+ leaderID);

          mLog.insert(entries, -1, 0);
          mCommitIndex = 1;
        }
        RaftServerImpl.setMode(new FollowerMode());

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
      if(timerID==0){
        //System.out.println("election timer S: "+ mID);
        int[] votes = RaftResponses.getVotes(mConfig.getCurrentTerm());
        int numVotes = 0;
        for(int i=1; i<= mConfig.getNumServers(); i++){
          if(votes!=null){
          if(votes[i]== 0){
            //System.out.println("votes" + votes[i] + "for candidate" + mID);
            numVotes+=1;
          }
        }
      }
        if(numVotes> mConfig.getNumServers()/2.0){
          hb_timer.cancel();
          //System.out.println("won boi");
          RaftServerImpl.setMode(new LeaderMode());
      }
        else{
          hb_timer.cancel();
          int term = mConfig.getCurrentTerm() + 1;
          mConfig.setCurrentTerm(term, mID);
          System.out.println ("S" +
            mID +
            "." +
            term +
            ": switched to candidate mode.");
        RaftResponses.setTerm(term);
        RaftResponses.clearVotes(term);
        RaftResponses.clearAppendResponses(term);

        Random rn = new Random();
        //random.nextInt(max-min+1)+min
        int time = rn.nextInt(ELECTION_TIMEOUT_MAX-ELECTION_TIMEOUT_MIN+1) +  ELECTION_TIMEOUT_MIN;

        elec_tim = scheduleTimer(Long.valueOf(time), 0);
        hb_timer = scheduleTimer(Long.valueOf(75), 1);
        for(int i=1; i<= mConfig.getNumServers(); i++){
          remoteRequestVote(i, term, mID, mLog.getLastIndex(), mLog.getLastTerm());
        }



      }
    }
    if(timerID==1){
      //System.out.println("polling timer S: "+ " " + mID);
      int[] votes = RaftResponses.getVotes(mConfig.getCurrentTerm());
      int numVotes = 0;
      for(int i=1; i<= mConfig.getNumServers(); i++){
        if(votes[i]== 0){
          ////System.out.println("votes" + votes[i] + "for candidate" + mID);
          numVotes+=1;
          }
      }
      if(numVotes> mConfig.getNumServers()/2.0){
        RaftServerImpl.setMode(new LeaderMode());
      }
      else{
        //System.out.println("no dice");
        hb_timer = scheduleTimer(Long.valueOf(75), 1);
      }
    }
  }
}
}
