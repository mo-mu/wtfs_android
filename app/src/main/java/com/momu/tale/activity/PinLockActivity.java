package com.momu.tale.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;
import com.momu.tale.R;
import com.momu.tale.config.CConfig;
import com.momu.tale.preference.AppPreference;
import com.momu.tale.utility.LogHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 잠금 화면
 * Created by knulps on 2017. 6. 3..
 */

public class PinLockActivity extends AppCompatActivity {
    @BindView(R.id.pin_lock_view) PinLockView mPinLockView;
    @BindView(R.id.indicator_dots) IndicatorDots indicatorDots;
    @BindView(R.id.txt_header) TextView txtHeader;
    @BindView(R.id.txt_title) TextView txtTitle;

    Context mContext = this;
    Vibrator vibrator;

    boolean isLockMode = false; //잠금 확인모드인지 여부
    boolean isCheckAgain = false;   //비밀번호 재확인 단계인지 여부
    String pinNumber = null;

    private static final String TAG = "PinLockActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_pinlock);
        ButterKnife.bind(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setTypeFace();

        mPinLockView.attachIndicatorDots(indicatorDots);
        mPinLockView.setPinLockListener(mPinLockListener);

        initLayout();
    }

    /**
     * 모드에 따른 레이아웃 초기화
     * 1. 잠금 해제 모드
     * 2. 잠금 비밀번호 재확인 모드
     * 3. 잠금 비밀번호 첫설정 혹은 변경 모드
     */
    void initLayout() {
        if (getIntent().getBooleanExtra("isLockMode", false)) {  //잠금 해제 모드인 경우
            txtHeader.setText(R.string.lock_pw_insert);
            isLockMode = true;

        } else if (getIntent().getBooleanExtra("checkAgain", false)) {  //비밀번호 재확인인 경우
            txtHeader.setText(R.string.lock_recheck);
            isCheckAgain = true;
            pinNumber = getIntent().getStringExtra("pinNumber");

        } else {    //잠금 비밀번호 첫설정 혹은 변경인 경우
            txtHeader.setText(R.string.lock_pw_insert);
        }
    }

    /**
     * 폰트 설정
     */
    void setTypeFace() {
        Typeface typeFace1 = Typeface.createFromAsset(mContext.getAssets(), CConfig.FONT_SEOUL_NAMSAN_CL);
        txtHeader.setTypeface(typeFace1);
        txtTitle.setTypeface(typeFace1);
    }

    private PinLockListener mPinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            LogHelper.e(TAG, "Pin complete: " + pin);
            indicatorDots.setPinLength(pin.length());

            if (isLockMode) {    //비밀번호 잠금 해제모드인 경우
                if (pin.equals(AppPreference.loadScreenPinNumber(mContext))) {  //비밀번호 일치한 경우
                    if (getIntent().getBooleanExtra("isStartApplication", false)) {  //앱 처음 시작인 경우 메인 엑티비티 실행해 준다.
                        //메인 페이지 시작
                        Intent intent = new Intent(mContext, MainActivity.class);
                        intent.putExtra("fragmentName", MainActivity.MAIN_FRAGMENT_MAIN);
                        startActivity(intent);
                    }

                } else {    //비밀번호 틀린 경우
                    Toast.makeText(mContext,R.string.toast_lock_pw_incorrect, Toast.LENGTH_SHORT).show();

                    vibrator.vibrate(500);
                    Intent lockIntent = new Intent(mContext, PinLockActivity.class);
                    lockIntent.putExtra("isLockMode", true);

                    if (getIntent().getBooleanExtra("isStartApplication", false)) {  //앱 처음 시작인 경우
                        lockIntent.putExtra("isStartApplication", true);
                    }

                    startActivity(lockIntent);
                }

            } else if (isCheckAgain) {  //비밀번호 재확인인 경우
                if (pinNumber != null && pinNumber.equals(pin)) {   //핀넘버 일치
                    Toast.makeText(mContext, R.string.toast_lock_setting_done, Toast.LENGTH_SHORT).show();
                    AppPreference.saveScreenPinNumber(mContext, pin);

                } else {    //핀넘버 틀림
                    Toast.makeText(mContext, R.string.toast_lock_pw_incorrect, Toast.LENGTH_SHORT).show();
                    vibrator.vibrate(500);

                    Intent lockIntent = new Intent(mContext, PinLockActivity.class);
                    startActivity(lockIntent);
                }

            } else {    //비밀번호 처음 입력인 경우 재확인을 위해 잠금화면 다시 보여줌
                Intent checkAgainIntent = new Intent(mContext, PinLockActivity.class);
                checkAgainIntent.putExtra("pinNumber", pin);
                checkAgainIntent.putExtra("checkAgain", true);
                startActivity(checkAgainIntent);
            }

            finish();
        }

        @Override
        public void onEmpty() {
            LogHelper.e(TAG, "Pin empty");
        }

        @Override
        public void onPinChange(int pinLength, String intermediatePin) {
            LogHelper.e(TAG, "Pin changed, new length " + pinLength + " with intermediate pin " + intermediatePin);
        }
    };

    @Override
    public void onBackPressed() {
        if (isLockMode) {   //잠금 해제 화면인 경우 백버튼을 누르면 홈버튼 누른 효과 나도록 한다.
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);

        } else {    //잠금 비밀번호 설정 화면인 경우
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
