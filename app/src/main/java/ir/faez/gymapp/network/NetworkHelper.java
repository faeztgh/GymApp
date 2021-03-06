package ir.faez.gymapp.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import ir.faez.gymapp.R;
import ir.faez.gymapp.data.model.Course;
import ir.faez.gymapp.data.model.CourseReservation;
import ir.faez.gymapp.data.model.ResultCourse;
import ir.faez.gymapp.data.model.ResultCourseReservation;
import ir.faez.gymapp.data.model.ResultReviews;
import ir.faez.gymapp.data.model.Review;
import ir.faez.gymapp.data.model.User;
import ir.faez.gymapp.utils.Result;
import ir.faez.gymapp.utils.ResultListener;

public class NetworkHelper {

    private static final String TAG = "NETWORK_HELPER";
    private static NetworkHelper instance = null;
    private Context context;
    private Gson gson = new Gson();
    private RequestQueue requestQueue;
    private String appId;
    private String apiKey;
    private String hostUrl;


    private NetworkHelper(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        this.appId = "";
        this.apiKey = "";
        this.hostUrl = "https://parseapi.back4app.com";
    }

    public static NetworkHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkHelper(context);
        }
        return instance;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ((ni != null) && ni.isConnected());
    }


    private void printVolleyErrorDetailes(VolleyError volleyError) {
        NetworkResponse errResponse = (volleyError != null) ? volleyError.networkResponse : null;
        int statusCode = 0;
        String data = "";

        if (errResponse != null) {
            statusCode = errResponse.statusCode;
            byte[] bytes = errResponse.data;
            data = (bytes != null) ? new String(bytes, StandardCharsets.UTF_8) : "";

        }

        Log.e(TAG, "Volley error with status code " + statusCode +
                " received with this message: " + data);


    }

    // **************************************** Sign Up ********************************

    public void signupUser(final User user, final ResultListener<User> listener) {
        if (!isNetworkConnected()) {
            Error error = new Error(context.getString(R.string.networkConnectionError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }

        String url = hostUrl + "/users";
        String userJson = null;
        try {
            userJson = gson.toJson(user);
        } catch (Exception ex) {
            ex.printStackTrace();
            Error error = new Error(context.getString(R.string.networkJsonError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }


        Response.Listener<String> responseListener = response -> {
            if (TextUtils.isEmpty(response)) {
                Error error = new Error(context.getString(R.string.networkGeneralError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            User resultUser;
            try {
                resultUser = gson.fromJson(response, new TypeToken<User>() {
                }.getType());

            } catch (Exception ex) {
                Error error = new Error(context.getString(R.string.networkJsonError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            listener.onResult(new Result<>(resultUser, null, null));
        };

        Response.ErrorListener errorListener = error -> {
            printVolleyErrorDetailes(error);

            // checking for username or email existence
            NetworkResponse errResponse = (error != null) ? error.networkResponse : null;
            String data = "";
            String myMessage = null;
            if (errResponse != null) {
                byte[] bytes = errResponse.data;
                data = (bytes != null) ? new String(bytes, StandardCharsets.UTF_8) : "";
                if (data.substring(8, 11).equals("202")) {
                    myMessage = "Username Exist!";
                } else if (data.substring(8, 11).equals("203")) {
                    myMessage = "Email Exist!";
                } else {
                    myMessage = context.getString(R.string.networkGeneralError);
                }
            }

            Error err = new Error(myMessage);
            listener.onResult(new Result<>(null, null, err));
            return;
        };

        final String jsonStr = userJson;
        StringRequest request = new StringRequest(Request.Method.POST, url, responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Parse-Application-Id", appId);
                headers.put("X-Parse-REST-API-Key", apiKey);
                headers.put("X-Parse-Revocable-Session", "1");
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return jsonStr.getBytes(StandardCharsets.UTF_8);
            }
        };
        requestQueue.add(request);
    }


    //************************************************* SIGN In ***********************

    public void signinUser(final User user, final ResultListener<User> listener) {
        if (!isNetworkConnected()) {
            Error error = new Error(context.getString(R.string.networkConnectionError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }

        String url = hostUrl + "/login?username=" + user.getUsername() + "&password="
                + user.getPassword();

        Response.Listener<String> responseListener = response -> {
            Log.d(TAG, "User signin response: " + response);
            if (TextUtils.isEmpty(response)) {
                Error error = new Error(context.getString(R.string.networkGeneralError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            User resultUser;
            try {
                resultUser = gson.fromJson(response, new TypeToken<User>() {
                }.getType());

            } catch (Exception ex) {
                ex.printStackTrace();
                Error error = new Error(context.getString(R.string.networkJsonError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            listener.onResult(new Result<>(resultUser, null, null));
        };
        Response.ErrorListener errorListener = error -> {
            printVolleyErrorDetailes(error);

            // check for user/pass validation
            NetworkResponse errResponse = (error != null) ? error.networkResponse : null;
            String data = "";
            String myMessage = null;
            if (errResponse != null) {
                byte[] bytes = errResponse.data;
                data = (bytes != null) ? new String(bytes, StandardCharsets.UTF_8) : "";
                if (data.substring(8, 11).equals("101")) {
                    myMessage = "Username or Password is Invalid";
                } else {
                    myMessage = context.getString(R.string.networkGeneralError);
                }
            }
            Error err = new Error(myMessage);
            listener.onResult(new Result<>(null, null, err));
        };

        StringRequest request = new StringRequest(Request.Method.GET, url, responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-Parse-Application-Id", appId);
                headers.put("X-Parse-REST-API-Key", apiKey);
                headers.put("X-Parse-Revocable-Session", "1");
                return headers;
            }
        };
        requestQueue.add(request);
    }

    //******************************************** Get All Courses ****************************
    public void getAllCourses(final ResultListener<Course> listener) {
        if (!isNetworkConnected()) {
            Error error = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }

        String url = hostUrl + "/classes/Course";

        Response.Listener<String> responseListener = response -> {
            Log.d(TAG, "Get All Courses response: " + response);
            if (TextUtils.isEmpty(response)) {
                Error error = new Error(context.getString(R.string.networkGeneralError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            ResultCourse resultCourses;

            try {
                resultCourses = gson.fromJson(response, new TypeToken<ResultCourse>() {
                }.getType());

                if (resultCourses.results == null) {
                    return;
                }


            } catch (Exception ex) {
                ex.printStackTrace();
                Error error = new Error(context.getString(R.string.networkJsonError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            listener.onResult(new Result<>(null, resultCourses.results, null));
        };

        Response.ErrorListener errorListener = error -> {
            printVolleyErrorDetailes(error);
            Error err = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, err));
        };


        StringRequest request = new StringRequest(Request.Method.GET, url, responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Parse-Application-Id", appId);
                headers.put("X-Parse-REST-API-Key", apiKey);
                return headers;
            }


        };
        requestQueue.add(request);
    }


    // ************************************* Insert CourseReservation ****************************

    public void insertCourseReservation(final CourseReservation courseReservation, final User currUser,
                                        final ResultListener<CourseReservation> listener) {

        if (!isNetworkConnected()) {
            Error error = new Error(context.getString(R.string.networkConnectionError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }


        String url = hostUrl + "/classes/CourseReservation";
        String expJson = null;
        try {
            expJson = gson.toJson(courseReservation);
        } catch (Exception ex) {
            ex.printStackTrace();
            Error error = new Error(context.getString(R.string.networkJsonError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }

        Response.Listener<String> responseListener = response -> {
            Log.d(TAG, "Course reservation insert response: " + response);
            if (TextUtils.isEmpty(response)) {
                Error error = new Error(context.getString(R.string.networkGeneralError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            CourseReservation courseReservationResult;
            try {
                courseReservationResult = gson.fromJson(response, new TypeToken<CourseReservation>() {
                }.getType());
            } catch (Exception ex) {
                ex.printStackTrace();
                Error error = new Error(context.getString(R.string.networkJsonError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            listener.onResult(new Result<>(courseReservationResult, null, null));
        };
        Response.ErrorListener errorListener = error -> {
            printVolleyErrorDetailes(error);
            Error err = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, err));
            return;
        };

        final String jsonStr = expJson;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Parse-Application-Id", appId);
                headers.put("X-Parse-REST-API-Key", apiKey);
                headers.put("X-Parse-Session-Token", currUser.getSessionToken());
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return jsonStr.getBytes(StandardCharsets.UTF_8);
            }
        };
        requestQueue.add(request);
    }

    // ************************************* Insert Review ****************************

    public void insertReview(final Review review, final User currUser,
                             final ResultListener<Review> listener) {

        if (!isNetworkConnected()) {
            Error error = new Error(context.getString(R.string.networkConnectionError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }


        String url = hostUrl + "/classes/Review";
        String expJson = null;
        try {
            expJson = gson.toJson(review);
        } catch (Exception ex) {
            ex.printStackTrace();
            Error error = new Error(context.getString(R.string.networkJsonError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }

        Response.Listener<String> responseListener = response -> {
            Log.d(TAG, "Review insert response: " + response);
            if (TextUtils.isEmpty(response)) {
                Error error = new Error(context.getString(R.string.networkGeneralError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            Review reviewResult;
            try {
                reviewResult = gson.fromJson(response, new TypeToken<Review>() {
                }.getType());
            } catch (Exception ex) {
                ex.printStackTrace();
                Error error = new Error(context.getString(R.string.networkJsonError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            listener.onResult(new Result<>(reviewResult, null, null));
        };
        Response.ErrorListener errorListener = error -> {
            printVolleyErrorDetailes(error);
            Error err = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, err));
            return;
        };

        final String jsonStr = expJson;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Parse-Application-Id", appId);
                headers.put("X-Parse-REST-API-Key", apiKey);
                headers.put("X-Parse-Session-Token", currUser.getSessionToken());
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return jsonStr.getBytes(StandardCharsets.UTF_8);
            }
        };
        requestQueue.add(request);
    }


    //******************************************** Get All Reviews *********************************
    public void getAllReviews(final ResultListener<Review> listener) {
        if (!isNetworkConnected()) {
            Error error = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<Review>(null, null, error));
            return;
        }

        String url = hostUrl + "/classes/Review";

        Response.Listener<String> responseListener = response -> {
            Log.d(TAG, "Get All Reviews response: " + response);
            if (TextUtils.isEmpty(response)) {
                Error error = new Error(context.getString(R.string.networkGeneralError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            ResultReviews resultReviews;

            try {
                resultReviews = gson.fromJson(response, new TypeToken<ResultReviews>() {
                }.getType());

                if (resultReviews.results == null) {
                    return;
                }


            } catch (Exception ex) {
                ex.printStackTrace();
                Error error = new Error(context.getString(R.string.networkJsonError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            listener.onResult(new Result<>(null, resultReviews.results, null));
        };

        Response.ErrorListener errorListener = error -> {
            printVolleyErrorDetailes(error);
            Error err = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, err));
        };


        StringRequest request = new StringRequest(Request.Method.GET, url, responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Parse-Application-Id", appId);
                headers.put("X-Parse-REST-API-Key", apiKey);
                return headers;
            }


        };
        requestQueue.add(request);
    }


    //*************************************** Get Specific CourseReservation ***********************


    public void getSpecificCourseReservationByOwnerId(final User user, final ResultListener<CourseReservation> listener) {
        if (!isNetworkConnected()) {
            Error error = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }


        String url = hostUrl + "/classes/CourseReservation?where={\"ownerId\":\"" + user.getId() + "\"}";
        String specificCourseReservationJson = null;


        Response.Listener<String> responseListener = response -> {
            Log.d(TAG, "Get Specific CourseReservation response: " + response);
            if (TextUtils.isEmpty(response)) {
                Error error = new Error(context.getString(R.string.networkGeneralError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            ResultCourseReservation resultCourseReservation;
            try {
                resultCourseReservation = gson.fromJson(response, new TypeToken<ResultCourseReservation>() {
                }.getType());


                if (resultCourseReservation.results == null) {
                    return;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                Error error = new Error(context.getString(R.string.networkJsonError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            listener.onResult(new Result<>(null, resultCourseReservation.results, null));
        };

        Response.ErrorListener errorListener = error -> {
            printVolleyErrorDetailes(error);
            Error err = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, err));
        };

        StringRequest request = new StringRequest(Request.Method.GET, url, responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Parse-Application-Id", appId);
                headers.put("X-Parse-REST-API-Key", apiKey);
                headers.put("X-Parse-Session-Token", user.getSessionToken());

                return headers;
            }
        };
        requestQueue.add(request);
    }

    //  ********************************************  Delete Reservation **************************
    public void deleteCourseReservation(final CourseReservation courseReservation,
                                        final User currentUser, final ResultListener<CourseReservation> listener) {
        if (!isNetworkConnected()) {
            Error error = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }

        String url = hostUrl + "/classes/CourseReservation/" + courseReservation.getId();
        String courseReservationJson;
        try {
            courseReservationJson = gson.toJson(courseReservation);
        } catch (Exception ex) {
            ex.printStackTrace();
            Error error = new Error(context.getString(R.string.networkJsonError));
            listener.onResult(new Result<>(null, null, error));
            return;
        }

        Response.Listener<String> responseListener = response -> {
            Log.d(TAG, "CourseReservation delete response: " + response);
            if (TextUtils.isEmpty(response)) {
                Error error = new Error(context.getString(R.string.networkGeneralError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            CourseReservation resultCourseReservation;
            try {
                resultCourseReservation = gson.fromJson(response, new TypeToken<CourseReservation>() {
                }.getType());
            } catch (Exception ex) {
                ex.printStackTrace();
                Error error = new Error(context.getString(R.string.networkJsonError));
                listener.onResult(new Result<>(null, null, error));
                return;
            }

            listener.onResult(new Result<>(resultCourseReservation,
                    null, null));
        };

        Response.ErrorListener errorListener = error -> {
            printVolleyErrorDetailes(error);
            Error err = new Error(context.getString(R.string.networkGeneralError));
            listener.onResult(new Result<>(null, null, err));
        };

        final String jsonStr = courseReservationJson;
        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("X-Parse-Application-Id", appId);
                headers.put("X-Parse-REST-API-Key", apiKey);
                headers.put("X-Parse-Session-Token", currentUser.getSessionToken());
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return jsonStr.getBytes(StandardCharsets.UTF_8);
            }
        };
        requestQueue.add(request);
    }

}
