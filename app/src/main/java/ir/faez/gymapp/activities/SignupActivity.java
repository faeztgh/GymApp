package ir.faez.gymapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ir.faez.gymapp.R;
import ir.faez.gymapp.data.model.User;
import ir.faez.gymapp.databinding.ActivitySignupBinding;
import ir.faez.gymapp.network.NetworkHelper;
import ir.faez.gymapp.utils.Result;
import ir.faez.gymapp.utils.ResultListener;

public class SignupActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SIGNUP";
    private ActivitySignupBinding binding;
    private NetworkHelper networkHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        // initializing binding
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // initializing NetworkHelper
        networkHelper = NetworkHelper.getInstance(getApplicationContext());

        // invoke Listeners
        invokeOnClickListeners();
    }


    private void invokeOnClickListeners() {
        binding.registerBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_btn:
                signUpBtnHandler();
                break;
            default:
                Toast.makeText(this, "Wrong Choice", Toast.LENGTH_SHORT).show();
        }
    }


    //****************************** Implementing OnClick Methods*********************************
    private void signUpBtnHandler() {
        String fullName = binding.fullnameEdt.getText().toString().trim();
        String email = binding.emailEdt.getText().toString().trim();
        String userName = binding.usernameEdt.getText().toString().trim();
        String password = binding.passwordEdt.getText().toString().trim();


        // validate fields
        if (isInputsFilled()
                && isEmailValid()
                && isPasswordValid()
                && isConfirmPasswordValid()) {

            User user = new User(fullName, email, userName, password);

            networkHelper.signupUser(user, new ResultListener<User>() {
                @Override
                public void onResult(Result<User> result) {
                    Log.d(TAG, "Result of signing user up in server" + result);
                    Error error = (result != null) ? result.getError() : null;

                    if ((result == null) || (error != null)) {
                        String errMsg = (error != null) ? error.getMessage() : getString(R.string.cantSignUpError);
                        Toast.makeText(SignupActivity.this, errMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(SignupActivity.this, R.string.successfulRegister, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

            Log.i(TAG, "signUpBtnHandler: "+userName);

        }
    }


    //****************************** Validating inputs *********************************
    private boolean isEmailValid() {
        //pattern src: https://www.tutorialspoint.com/how-to-check-email-address-validation-in-android-on-edit-text

        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (binding.emailEdt.getText().toString().trim().matches(emailPattern)
                && !binding.emailEdt.getText().toString().trim().isEmpty()) {
            return true;
        } else {
            binding.emailEdt.setError("Wrong email format");
            return false;
        }
    }

    private boolean isPasswordValid() {

        if (binding.passwordEdt.getText().toString().length() >= 6
                && !binding.passwordEdt.getText().toString().trim().isEmpty()) {
            return true;
        } else {
            binding.passwordEdt.setError("Password length must be 6 or more");
            return false;
        }
    }


    private boolean isConfirmPasswordValid() {
        if (binding.passwordRepEdt.getText().toString()
                .equals(binding.passwordEdt.getText().toString())
                && !binding.passwordRepEdt.getText().toString().trim().isEmpty()) {
            return true;
        } else {
            binding.passwordRepEdt.setError("Passwords not match");
            Toast.makeText(SignupActivity.this,
                    "Confirm password not match!"
                    , Toast.LENGTH_SHORT).show();

            return false;
        }
    }


    private boolean isInputsFilled() {
        if (!binding.fullnameEdt.getText().toString().trim().isEmpty()
                && !binding.emailEdt.getText().toString().trim().isEmpty()
                && !binding.usernameEdt.getText().toString().trim().isEmpty()
                && !binding.passwordEdt.getText().toString().trim().isEmpty()
                && !binding.passwordRepEdt.getText().toString().trim().isEmpty()) {
            return true;
        } else {
            Toast.makeText(SignupActivity.this,
                    "Please Complete The Form",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

}
