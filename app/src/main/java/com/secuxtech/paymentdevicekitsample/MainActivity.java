package com.secuxtech.paymentdevicekitsample;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.secuxtech.paymentdevicekit.*;
import static com.secuxtech.paymentdevicekit.PaymentPeripheralManager.SecuX_Peripheral_Operation_OK;

import org.json.JSONObject;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;




public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Payment Test Sample";
    private final Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    public void onPayButtonClick(View v){
        EditText editTextDevID = findViewById(R.id.editText_device_id);
        if (editTextDevID.getText().length()==0){
            Toast toast = Toast.makeText(this, "Please input the device ID!", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            return;
        }

        EditText editTextAmount = findViewById(R.id.editText_amount);
        if (editTextAmount.getText().length() == 0 || Float.valueOf(editTextAmount.getText().toString()) <= 0){
            Toast toast = Toast.makeText(this, "Invalid payment amount!", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            return;
        }

        final String devID = editTextDevID.getText().toString();
        final String amount = editTextAmount.getText().toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                PaymentPeripheralManager peripheralManager = new PaymentPeripheralManager();
                //peripheralManager.doGetIVKey(mContext, 10000, "4ab10000726b", -80, 10000);
                Pair<Integer, String> getIVKeyret = peripheralManager.doGetIVKey(mContext, 5, devID, -80, 15);
                String ivKey = "";
                if (getIVKeyret.first == SecuX_Peripheral_Operation_OK) {
                    ivKey = getIVKeyret.second;
                    Log.i(TAG, "ivKey=" + ivKey);
                } else {
                    Log.i(TAG, "Get ivKey failed! Error:  " + getIVKeyret.second);

                    return;
                }

                try {
                    JSONObject ioCtrlParamJson = new JSONObject("{\"uart\":\"0\",\"gpio1\":\"0\",\"gpio2\":\"0\",\"gpio31\":\"0\",\"gpio32\":\"0\",\"gpio4\":\"0\",\"gpio4c\":\"0\",\"gpio4cInterval\":\"0\",\"gpio4cCount\":\"0\",\"gpio4dOn\":\"0\",\"gpio4dOff\":\"0\",\"gpio4dInterval\":\"0\",\"runStatus\":\"0\",\"lockStatus\":\"0\"}");

                    final MachineIoControlParam machineIoControlParam = new MachineIoControlParam();
                    machineIoControlParam.setGpio1(ioCtrlParamJson.getString("gpio1"));
                    machineIoControlParam.setGpio2(ioCtrlParamJson.getString("gpio2"));
                    machineIoControlParam.setGpio31(ioCtrlParamJson.getString("gpio31"));
                    machineIoControlParam.setGpio32(ioCtrlParamJson.getString("gpio32"));
                    machineIoControlParam.setGpio4(ioCtrlParamJson.getString("gpio4"));
                    machineIoControlParam.setGpio4c(ioCtrlParamJson.getString("gpio4c"));
                    machineIoControlParam.setGpio4cCount(ioCtrlParamJson.getString("gpio4cCount"));
                    machineIoControlParam.setGpio4cInterval(ioCtrlParamJson.getString("gpio4cInterval"));
                    machineIoControlParam.setGpio4dOn(ioCtrlParamJson.getString("gpio4dOn"));
                    machineIoControlParam.setGpio4dOff(ioCtrlParamJson.getString("gpio4dOff"));
                    machineIoControlParam.setGpio4dInterval(ioCtrlParamJson.getString("gpio4dInterval"));
                    machineIoControlParam.setUart(ioCtrlParamJson.getString("uart"));
                    machineIoControlParam.setRunStatus(ioCtrlParamJson.getString("runStatus"));
                    machineIoControlParam.setLockStatus(ioCtrlParamJson.getString("lockStatus"));

                    //String encryptedStr = "91sGnngVALh6Ep3RsEJKhGQEQM2ntJiZxR0cwiQNLT\\/SbZcCVux1WHabIrzqkICsPz3PudpRHnEFwcbiMO7qhA==";
                    //final byte[] encryptedData = Base64.decode(encryptedStr, Base64.DEFAULT);
                    final byte[] encryptedData = getEncryptMobilePaymentCommand(devID.substring(devID.length() - 8, devID.length()), amount, ivKey, "PA123456789012345678901234567890");

                    Pair<Integer, String> ret = peripheralManager.doPaymentVerification(encryptedData, machineIoControlParam);
                    if (ret.first != SecuX_Peripheral_Operation_OK) {
                        Log.i(TAG, "Payment failed! " + ret.second);
                    } else {
                        Log.i(TAG, "Payment done");
                    }
                } catch (Exception e) {
                    Log.i(TAG, "Generate io config failed!");
                }

            }
        }).start();
    }

    public byte[] getEncryptMobilePaymentCommand(String terminalId, String amount, String ivKey, String cryptKey){
        Log.d(TAG,"getEncryptMobilePaymentCommand()");
        String plainTransaction = getMobilePaymentCommand(terminalId, amount);

        // AES 256 crypt
        byte[] encrytedTransactionData = null;
        try {
            byte[] ivKeyData = ivKey.getBytes();
            encrytedTransactionData = encrypt(ivKeyData, cryptKey.getBytes(), plainTransaction.getBytes());
        }catch (Exception ex) {
            encrytedTransactionData = null;
        }

        return encrytedTransactionData;
    }

    public byte[] encrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes)
            throws java.io.UnsupportedEncodingException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {

        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
        return cipher.doFinal(textBytes);
    }

    public String getMobilePaymentCommand(String terminalId, String amount) {
        Log.d(TAG,"======= getMobilePaymentCommand() =======");
        Calendar c = Calendar.getInstance();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmmss");
        String transactionTime = format1.format(c.getTime());
        String paymentID = "P12345678912" + transactionTime.substring(transactionTime.length() - 4);
        String amountString = convertAmountToFourDigits(amount);
        String amountCurrency = "DCT";
        if(amountCurrency.length() == 0) amountCurrency = "TWD";
        String plainTransaction = transactionTime + "," + paymentID + "," + terminalId + "," + amountString+","+amountCurrency;

        Log.d(TAG,"plainTransaction:"+plainTransaction);
        return plainTransaction;
    }

    private String convertAmountToFourDigits(String amount) {
        int length = String.valueOf(amount).length();

        String amountStr = amount+"";
        int Remaining = 8-length;
        for(int i=0;i<Remaining;i++){
            amountStr = " "+amountStr;
        }

//        if (length == 1) {
//            return "   " + amount;
//        }
//
//        if (length == 2) {
//            return "  " + amount;
//        }
//
//        if (length == 3) {
//            return " " + amount;
//        }

        // length is 8
        Log.d(TAG,"convertAmountToFourDigits: "+amountStr);
        Log.d(TAG,"convertAmountToFourDigits: "+amountStr.length());
        return amountStr;
    }
}
