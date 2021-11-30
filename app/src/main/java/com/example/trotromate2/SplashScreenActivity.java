package com.example.trotromate2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;

public class SplashScreenActivity extends AppCompatActivity {
    private static final  int REQUEST_CODE = 700;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private FirebaseDatabase database;
    private DatabaseReference driverInfoRef;

    @BindView(R.id.progress__bar)
    ProgressBar progressBar;
    @Override
    protected void onStart() {
        super.onStart();
        displayScreen();
    }

    @Override
    protected void onStop() {
        if(firebaseAuth != null && listener != null){
            firebaseAuth.removeAuthStateListener(listener);
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen_activity);

        initiate();

    }

    private void initiate() {
        ButterKnife.bind(this);
        database = FirebaseDatabase.getInstance();
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCES);

        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth ->{
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user != null){
                checkFromTheDatabase();
            }
            else{
                showLoginLayout();
            }
        };
    }

    private void checkFromTheDatabase() {
        driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            // Toast.makeText(SplashScreenActivity.this, "Users Already Register", Toast.LENGTH_SHORT).show();
                            DriverInfoModel driverInfoModel = snapshot.getValue(DriverInfoModel.class);
                            goToHomePageActivity(driverInfoModel);
                        }
                        else{
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.DialogTheme);
        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null);
//set data
        TextInputEditText first__name = (TextInputEditText)itemView.findViewById(R.id.first_name);
        TextInputEditText last__name = (TextInputEditText)itemView.findViewById(R.id.last_name);
        TextInputEditText phoneNo= (TextInputEditText)itemView.findViewById(R.id.phone_number);
        Button btn__register =(Button)itemView.findViewById(R.id.btn_register);
        if(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() !=null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
            phoneNo.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
        //set View
        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();
        btn__register.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (TextUtils.isEmpty(first__name.getText().toString())) {
                    first__name.setError("First name is Required");

                } else if (TextUtils.isEmpty(last__name.getText().toString())) {
                    last__name.setError("last name is Required");

                } else if (TextUtils.isEmpty(phoneNo.getText().toString())) {
                    phoneNo.setError("Phone number Required");


                }else{
                    DriverInfoModel driverInfoModel = new DriverInfoModel();
                    driverInfoModel.setFirstname(first__name.getText().toString());
                    driverInfoModel.setLastname(last__name.getText().toString());
                    driverInfoModel.setPhoneNumber(phoneNo.getText().toString());


                    driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(driverInfoModel)
                            .addOnFailureListener(new OnFailureListener() {
                                public void onFailure(@NonNull Exception e) {
                                    dialog.dismiss();
                                    Toast.makeText(SplashScreenActivity.this,"Registration Failed",Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(SplashScreenActivity.this, "Registration was successful",Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    goToHomePageActivity(driverInfoModel);
                                }
                            });
                }


            }
        });

    }

    private void goToHomePageActivity(DriverInfoModel driverInfoModel) {
        Common.currentUser = driverInfoModel;
        startActivity(new Intent(SplashScreenActivity.this,DriverHomeActivity.class));
        finish();

    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.activity_login_in)
                .setPhoneButtonId(R.id.btn_sign_in_phone)
                .setGoogleButtonId(R.id.btn_sign_in_google)
                .build();

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setIsSmartLockEnabled(false).setAvailableProviders(providers)
                .build(),REQUEST_CODE);
    }

    private void displayScreen() {
        progressBar.setVisibility(View.VISIBLE);

        Completable.timer(5, TimeUnit.SECONDS
                , AndroidSchedulers.mainThread())
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        //if this the first time login or not then this will trigger
                        firebaseAuth.addAuthStateListener(listener);
                    }
                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE){
            IdpResponse idpResponse = IdpResponse.fromResultIntent(data);

            if(resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else{
                Toast.makeText(this, "Failed To Sign"+idpResponse.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


}