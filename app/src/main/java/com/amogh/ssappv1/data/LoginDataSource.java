package com.amogh.ssappv1.data;

import com.amogh.ssappv1.SSApp;
import com.amogh.ssappv1.data.model.LoggedInUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    public Result<LoggedInUser> login(String username, String password) {

        try {
            return new Result.Success<>(validateUser(username, password));
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }

    private LoggedInUser validateUser(String userName, String pass) throws Exception {
        JSONObject user = this.validateUserCred(userName, pass);
        LoggedInUser fakeUser =
                new LoggedInUser(
                        java.util.UUID.randomUUID().toString(),
                        (user.getString("fName")+" "+user.getString("lName")));

        return fakeUser;
    }

    private JSONObject validateUserCred(String userName, String pass) throws Exception {
        JSONObject user = this.readJSONFromAsset(userName);
        if( null == user) {
            throw new Exception("User not found");
        }
        if( !pass.equals(user.getString("uPass")) ) {
            throw new Exception ("Invalid user");
        }
        return user;
    }

    private JSONObject readJSONFromAsset(String userName) throws Exception {
        JSONObject usersJSON = null;
        try {
            InputStream is = SSApp.getContext().getAssets().open("cater.data");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            return this.getUserObject(new JSONObject(json), userName);
        } catch (IOException | JSONException ex) {
            throw new Exception(ex);
        }
    }

    private JSONObject getUserObject(JSONObject usersObj, String userName) throws Exception {
        try {
            JSONObject user = null;
            JSONArray users = usersObj.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                user = users.getJSONObject(i);
                if( user.get("uName").equals(userName) ) {
                    return user;
                }
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
        return null;
    }
}