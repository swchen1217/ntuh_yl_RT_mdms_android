package com.swchen1217.ntuh_yl_rt_mdms;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "debug";
    EditText et_acc, et_pw;
    Button btn_forget, btn_login, btn_change;
    CheckBox cb_rememberme;
    int login_error_count = 0;
    public static Boolean engineering_mode_SkipLogin = false;
    String server_url = "";
    String server_url_web = "";
    private long exitTime = 0;
    SharedPreferences spf_rememberme, spf_LoginInfo;
    ProgressDialog pd;

    @Override
    protected void onStart() {
        super.onStart();
        spf_LoginInfo.edit()
                .putString("acc", "")
                .putString("pw", "")
                .putString("name", "")
                .putString("permission", "")
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        spf_rememberme = getSharedPreferences("remember", MODE_PRIVATE);
        spf_LoginInfo = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        et_acc = findViewById(R.id.et_acc);
        et_pw = findViewById(R.id.et_pw);
        btn_forget = findViewById(R.id.btn_forgetpw);
        btn_login = findViewById(R.id.btn_login);
        btn_change = findViewById(R.id.btn_changepw);
        cb_rememberme = findViewById(R.id.cb_rememberme);

        et_acc.setText(spf_rememberme.getString("acc", ""));
        et_pw.setText(spf_rememberme.getString("pw", ""));
        if (spf_rememberme.getString("acc", "") != "")
            cb_rememberme.setChecked(true);

        getServerIP_check();

        setListener();
    }

    public void setListener() {
        btn_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getServerIP_check()) {
                    Uri uri = Uri.parse("http://" + server_url_web + "index.html#ChangePw");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
            }
        });

        btn_forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText input = new EditText(LoginActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("忘記密碼")
                        .setMessage("請輸入您註冊的Email")
                        .setView(input)
                        .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // 在此處理 input
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String fgtpw = PostDataToSrever("user.php",
                                                    new FormBody.Builder()
                                                            .add("mode", "forget_pw")
                                                            .add("email", input.getText().toString())
                                                            .build());
                                            if(fgtpw!=null){
                                                if(fgtpw.equals("寄信成功")){
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            new AlertDialog.Builder(LoginActivity.this)
                                                                    .setTitle("已將重設密碼資料傳送至Email")
                                                                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                        }
                                                                    })
                                                                    .show();
                                                        }
                                                    });
                                                }else if(fgtpw.equals("email_not_exist")){
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            new AlertDialog.Builder(LoginActivity.this)
                                                                    .setTitle("無此Email,請聯繫管理員!!")
                                                                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                        }
                                                                    })
                                                                    .show();
                                                        }
                                                    });
                                                }else{
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            new AlertDialog.Builder(LoginActivity.this)
                                                                    .setTitle("寄信系統錯誤,請聯繫管理員!!")
                                                                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                        }
                                                                    })
                                                                    .show();
                                                        }
                                                    });
                                                }
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                thread.start();
                            }
                        })
                        .show();
            }
        });
        btn_login.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (engineering_mode_SkipLogin == true && et_acc.getText().toString().equals("") && et_pw.getText().toString().equals("")) {
                    Toast.makeText(LoginActivity.this, "工程模式_SkipLogin", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MenuActivity.class));
                    finish();
                }
                if ((et_acc.getText().toString().equals("") || et_pw.getText().toString().equals("")) && engineering_mode_SkipLogin != true) {
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("帳號或密碼為空白!!")
                            .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();
                } else {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //Your code goes here
                                String login_st = PostDataToSrever("user.php",
                                        new FormBody.Builder()
                                                .add("mode", "get_create_time")
                                                .add("acc", et_acc.getText().toString())
                                                .build());
                                if (login_st != null) {
                                    if (login_st.equals("no_acc")) {
                                        Log.d("OkHttp", "login_check no_acc");
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                //Code goes here
                                                new AlertDialog.Builder(LoginActivity.this)
                                                        .setTitle("員工編號錯誤!!")
                                                        .setMessage("此員工編號尚未註冊")
                                                        .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                            }
                                                        })
                                                        .show();
                                                et_acc.setText("");
                                                et_pw.setText("");
                                            }
                                        });
                                    } else {
                                        String md5 = md5(login_st + et_pw.getText().toString());
                                        //Log.d("md5",md5);
                                        String login_nd = PostDataToSrever("user.php",
                                                new FormBody.Builder()
                                                        .add("mode", "login_check")
                                                        .add("acc", et_acc.getText().toString())
                                                        .add("pw", md5)
                                                        .build());
                                        if (login_nd != null) {
                                            if (login_nd.equals("no_enable")) {
                                                Log.d("OkHttp", "login_check no_enable");
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        new AlertDialog.Builder(LoginActivity.this)
                                                                .setTitle("此帳號尚未啟用,請聯繫管理員!!")
                                                                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                    }
                                                                })
                                                                .show();
                                                    }
                                                });
                                            }
                                            if (login_nd.substring(0, 2).equals("ok")) {
                                                if (cb_rememberme.isChecked()) {
                                                    spf_rememberme.edit()
                                                            .putString("acc", et_acc.getText().toString())
                                                            .putString("pw", et_pw.getText().toString())
                                                            .commit();
                                                } else {
                                                    spf_rememberme.edit()
                                                            .putString("acc", "")
                                                            .putString("pw", "")
                                                            .commit();
                                                }
                                                String[] split_OkData = login_nd.split(",");
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(LoginActivity.this, split_OkData[1] + " ,歡迎回來", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                spf_LoginInfo.edit()
                                                        .putString("acc", et_acc.getText().toString())
                                                        .putString("pw", md5)
                                                        .putString("name", split_OkData[1])
                                                        .putString("permission", split_OkData[2])
                                                        .commit();
                                                login_error_count = 0;
                                                startActivity(new Intent(LoginActivity.this, MenuActivity.class));
                                                finish();
                                            }
                                            if (login_nd.equals("pw_error")) {
                                                Log.d("OkHttp", "login_check pw_error");
                                                login_error_count++;
                                                if (login_error_count != 3) {
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            //Code goes here
                                                            new AlertDialog.Builder(LoginActivity.this)
                                                                    .setTitle("密碼錯誤!!")
                                                                    .setMessage("還有 " + (3 - login_error_count) + " 次機會")
                                                                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {

                                                                        }
                                                                    })
                                                                    .show();
                                                            et_pw.setText("");

                                                        }
                                                    });
                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            //Code goes here
                                                            new AlertDialog.Builder(LoginActivity.this)
                                                                    .setTitle("密碼錯誤!!")
                                                                    .setMessage("密碼錯誤 3 次")
                                                                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            finish();
                                                                        }
                                                                    })
                                                                    .show();
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                }
            }
        });
    }

    public String PostDataToSrever(String file, FormBody formBody) throws IOException {
        runOnUiThread(new Runnable() {
            public void run() {
                //Code goes here
                pd = new ProgressDialog(LoginActivity.this);
                pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pd.setMessage("與伺服器連線中...");
                pd.setCancelable(false);
            }
        });
        if (getServerIP_check()) {
            runOnUiThread(new Runnable() {
                public void run() {
                    //Code goes here
                    pd.show();
                }
            });
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient()
                    .newBuilder().addInterceptor(logging)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .dns(new OkHttpDns2(10000))
                    .build();
            Request request = new Request.Builder()
                    .url("http://" + server_url + file)
                    .post(formBody) // 使用post連線
                    .build();
                /*Request request = new Request.Builder()
                        .url("https://www.google.com/") //Google
                        .build();*/
            Call call = client.newCall(request);
            try (Response response = call.execute()) {
                if (response.code() == 200) {
                    return response.body().string();
                } else {
                    Toast.makeText(LoginActivity.this, "伺服器錯誤,請聯繫管理員", Toast.LENGTH_SHORT).show();
                    return null;
                }
            } catch (Exception e) {
                Log.d("OkHttp", "Error:" + e.toString());
                if (e instanceof UnknownHostException) {
                    OkHttpClient client2 = new OkHttpClient()
                            .newBuilder().addInterceptor(logging)
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .dns(new OkHttpDns2(10000))
                            .build();
                    Request request2 = new Request.Builder()
                            .url("https://www.google.com/") //Google
                            .build();
                    Call call2 = client2.newCall(request2);
                    try (Response response2 = call2.execute()) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                //Code goes here
                                Toast.makeText(LoginActivity.this, "無法連接至伺服器", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e2) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                //Code goes here
                                Toast.makeText(LoginActivity.this, "無法連接至網際網路", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return null;
                    }
                }
                if (e instanceof SocketTimeoutException) {
                    //判断超时异常
                    runOnUiThread(new Runnable() {
                        public void run() {
                            //Code goes here
                            Toast.makeText(LoginActivity.this, "無法連接至伺服器", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return null;
            } finally {
                runOnUiThread(new Runnable() {
                    public void run() {
                        //Code goes here
                        pd.dismiss();
                    }
                });
            }
        }
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {

            if (System.currentTimeMillis() - exitTime > 3000) {
                Toast.makeText(getApplicationContext(), "再按一次返回鍵退出", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, PrefsActivity.class));
                return true;
        }
        return false;
    }

    public boolean getServerIP_check() {
        if (PrefsActivity.getServer(LoginActivity.this) != "") {
            server_url = PrefsActivity.getServer(LoginActivity.this) + "/ntuh_yl_RT_mdms_api/";
            server_url_web = PrefsActivity.getServer(LoginActivity.this) + "/ntuh_yl_RT_mdms_web/";
            return true;
        } else {
            runOnUiThread(new Runnable() {
                public void run() {
                    //Code goes here
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("未設定伺服器位址!!")
                            .setMessage("請聯繫管理員取得伺服器位址")
                            .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(LoginActivity.this, PrefsActivity.class));
                                }
                            })
                            .show();
                }
            });
            return false;
        }
    }

    public String md5(String str) {
        Log.d("md5", "input:" + str);
        try {
            MessageDigest md = null;
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(str.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);

            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            Log.d("md5", "output:" + hashtext);
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}