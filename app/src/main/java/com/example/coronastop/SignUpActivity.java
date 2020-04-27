package com.example.coronastop;

import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;


public class SignUpActivity extends AppCompatActivity {
    private EditText id, password, password2, name, email, nickname, phoneNumber, businessNumber;
    private Button register, cancel, doubleCheck;
    private RadioButton ceo, customer;
    private RadioGroup isCEORadio;
    private LinearLayout businessLayout, resultLayout;
    private boolean idOK = false;
    private TextView doubleCheckResult;
    private long mLastClickTime = 0;

    // Access a Cloud Firestore instance from your Activity
    final FirebaseFirestore db = FirebaseFirestore.getInstance(); //파이어스토어
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_layout);

        id = findViewById(R.id.id_register);
        password = findViewById(R.id.password_register);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password2 = findViewById(R.id.password2);
        password2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        register = findViewById(R.id.register);
        cancel = findViewById(R.id.cancel);
        businessLayout = findViewById(R.id.layout);
        businessLayout.setVisibility(View.GONE);
        doubleCheck = findViewById(R.id.doubleCheck);
        doubleCheckResult = findViewById(R.id.doubleCheckResult);
        resultLayout = findViewById(R.id.resultLayout);
        resultLayout.setVisibility(View.GONE);
    }
    @Override
    protected void onStart() {
        super.onStart();

        register.setOnClickListener(new View.OnClickListener() { //회원가입 버튼을 눌렀을 때
            @Override
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) { return; } //버튼 중복 클릭 방지
                mLastClickTime = SystemClock.elapsedRealtime();

                DocumentReference docRef = db.collection("User").document(id.getText().toString()); //아이디 중복확인 //아직 아이디에 특수문자 확인못함
                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                doubleCheckResult.setText("아이디가 이미 존재합니다.");
                                doubleCheckResult.setTextColor(Color.RED);
                                resultLayout.setVisibility(View.VISIBLE);
                                idOK = false;
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, "아이디 중복확인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                if(TextUtils.isEmpty(id.getText())) {
                    Toast.makeText(SignUpActivity.this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
                else if(!idOK) {
                    Toast.makeText(SignUpActivity.this, "아이디 중복확인을 눌러주세요.", Toast.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(password.getText())) {
                    Toast.makeText(SignUpActivity.this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
                else if(!password.getText().toString().equals(password2.getText().toString())) {
                    Toast.makeText(SignUpActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
                else { //모두 입력 완료했다면
                    String idStr = id.getText().toString(),
                            passwordStr = password.getText().toString();
                    writeNewUser(idStr, passwordStr);
                    finish();
                }
            } //public void onClick
        }); //register.setOnClickListener

        cancel.setOnClickListener(new View.OnClickListener() { //최소 버튼을 눌렀을 때
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        doubleCheck.setOnClickListener(new View.OnClickListener() { //아이디 중복확인
            @Override
            public void onClick(View v) {

                DocumentReference docRef = db.collection("User").document(id.getText().toString());
                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                doubleCheckResult.setText("아이디가 이미 존재합니다."); //아이디가 데이터 베이스에 존재할 때
                                doubleCheckResult.setTextColor(Color.RED);
                                resultLayout.setVisibility(View.VISIBLE);
                                idOK = false;
                            } else {
                                doubleCheckResult.setText("이 아이디는 사용 가능합니다."); //아이디가 데이터 베이스에 존재하지 않을 때
                                doubleCheckResult.setTextColor(Color.BLUE);
                                resultLayout.setVisibility(View.VISIBLE);
                                idOK = true;
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, "중복확인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }); //docRef.get().addOnCompleteListener

            } //onClick
        }); //doubleCheck.setOnClickListener

    } //protected void onStart()

    private void writeNewUser(String userID,String password) {
        //User user = new User(name, email, password, nickname, phoneNumber, position, businessNumber, userID);

        // Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        user.put("userID", userID);
        user.put("userPwd", password);

        db.collection("User") //User Collection
                .document(userID) //userID의 Document
                .set(user) //user가 입력한 정보를 파이어베이스에 등록
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(SignUpActivity.this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignUpActivity.this, "회원가입에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    } //private void writeNewUser
}