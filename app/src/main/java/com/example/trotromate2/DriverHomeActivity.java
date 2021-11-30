package com.example.trotromate2;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.trotromate2.ui.home.userUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trotromate2.databinding.ActivityDriverHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class DriverHomeActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityDriverHomeBinding binding;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private  NavController navController;
    private StorageReference storageReference;
    private AlertDialog waiting;
    private static  int PICKER_REQUEST_CODE = 88;
    private Uri imageUri;
    private ImageView avatar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

       //setSupportActionBar(binding.appBarDriverHome.toolbar);

         drawer = binding.drawerLayout;
         navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_driver_home);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
       initiate();
    }

    private void initiate() {
        waiting = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("Waiting..")
                .create();
        storageReference = FirebaseStorage.getInstance().getReference();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.nav_sign_Out)
                {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(DriverHomeActivity.this);
                    dialog.setTitle("Sign Out")
                            .setMessage("Do you really want to Sign Out ? ")
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setPositiveButton("Sign Out", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    FirebaseAuth.getInstance().signOut();
                                    Intent intent = new Intent(DriverHomeActivity.this,SplashScreenActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            })
                            .setCancelable(false);
                    AlertDialog dialog1 = dialog.create();
                    dialog1.setOnShowListener(dialogInterface -> {
                        dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(getResources().getColor(R.color.black));
                        dialog1.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        dialog1.show();

                    });


                }
                return true;
            }
        });


        //set data
        View headerView = navigationView.getHeaderView(0);
        TextView txt_name = (TextView) headerView.findViewById(R.id.name_txt);
        TextView txt_phone = (TextView) headerView.findViewById(R.id.phone_number);
        TextView rating = (TextView) headerView.findViewById(R.id.rating_txt);
         avatar = (ImageView) headerView.findViewById(R.id.imageView_avatar);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(intent.ACTION_GET_CONTENT);
                startActivityForResult(intent,PICKER_REQUEST_CODE);
            }
        });

        txt_name.setText(Common.buildMessage());
        txt_phone.setText(Common.currentUser != null ? Common.currentUser.getPhoneNumber() : " ");
        rating.setText(Common.currentUser !=null ? String.valueOf(Common.currentUser.getRating()):"0.0");
        if(Common.currentUser != null && Common.currentUser.getAvatar() !=null &&
                !TextUtils.isEmpty(Common.currentUser.getAvatar())){
            Glide.with(this)
                    .load(Common.currentUser.getAvatar())
                    .into(avatar);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKER_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                if(data != null && data.getData() != null ){
                    imageUri = data.getData();
                    avatar.setImageURI(imageUri);

                    showDialogUpload();
                }
            }
        }
    }

    private void showDialogUpload() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DriverHomeActivity.this);
        builder.setTitle("Upload Image")
                .setMessage("Do you really want change your profile ? ")
                .setNegativeButton("Cancel ",((dialogInterface, i) -> {
                    dialogInterface.dismiss();
                }))
                .setPositiveButton("Upload ",(dialogInterface, i) -> {
                    if(imageUri !=null){
                        waiting.setMessage("Uploading..");
                        waiting.show();

                        String unique_name = FirebaseAuth.getInstance().getUid();
                        StorageReference imageFolder = storageReference.child("Images/"+unique_name);
                        imageFolder.putFile(imageUri)
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        waiting.dismiss();
                                        Snackbar.make(drawer,e.getMessage(),Snackbar.LENGTH_SHORT).show();
                                    }
                                }).addOnCompleteListener(task -> {
                                    if(task.isSuccessful()){
                                        imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                                            Map<String,Object> hashmap = new HashMap<>();
                                            hashmap.put("avatar",uri.toString());
                                            userUtils.updateUser(drawer,hashmap);
                                        });
                                    }
                                    waiting.dismiss(); 
                                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                double progress = (100 * snapshot.getTotalByteCount() /snapshot.getTotalByteCount());
                                waiting.setMessage(new StringBuilder("Uploading: ").append(progress).append("%"));
                            }
                        });
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.driver_home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_driver_home);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}