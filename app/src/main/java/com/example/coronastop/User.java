package com.example.coronastop;

public class User {

    public String userID, position;
    public String userPwd; //private로는 값 못가져오나..?
    //public boolean isCEO;


    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String name, String email, String userPwd, String nickname, String phoneNumber, String position, String businessNumber, String userID) {
        this.userPwd = userPwd;
        this.position = position;
        this.userID = userID;
    }

    public String getUserPwd() {
        return userPwd;
    }
    public String getPosition() { return position; }

}