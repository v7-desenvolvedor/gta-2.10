package com.samp.mobile.game;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import androidx.appcompat.app.AppCompatActivity; // Importante para o onCreate

import com.joom.paranoid.Obfuscate;
import com.samp.mobile.game.ui.AttachEdit;
import com.samp.mobile.game.ui.CustomKeyboard;
import com.samp.mobile.game.ui.LoadingScreen;
import com.samp.mobile.game.ui.dialog.DialogManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

@Obfuscate
public class SAMP extends AppCompatActivity { // Certifique-se que o nome da classe é SAMP
    
    private static final String TAG = "SAMP";
    private static SAMP instance;
    
    private CustomKeyboard mKeyboard;
    private DialogManager mDialog;
    private AttachEdit mAttachEdit;
    private LoadingScreen mLoadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "**** onCreate");
        
        // 1. Configura a captura de erros antes de tudo
        setupJavaLogCapture();

        super.onCreate(savedInstanceState);

        // 2. Inicializa componentes
        mKeyboard = new CustomKeyboard(this);
        mDialog = new DialogManager(this);
        mAttachEdit = new AttachEdit(this);
        mLoadingScreen = new LoadingScreen(this);

        instance = this;
        
        // 3. Checa permissões e tenta carregar a Lib nativa
        checkAllFilesPermission();

        try {
            initializeSAMP();
        } catch (UnsatisfiedLinkError e5) {
            saveLogToFile("UnsatisfiedLinkError: " + e5.getMessage());
            Log.e(TAG, "Erro ao carregar lib: " + e5.getMessage());
        } catch (Exception e) {
            saveLogToFile("Erro no onCreate: " + e.getMessage());
        }
    }

    // --- SISTEMA DE LOGS ---

    private void setupJavaLogCapture() {
        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                paramThrowable.printStackTrace(pw);
                
                saveLogToFile("--- FATAL JAVA CRASH ---\n" + sw.toString());

                if (oldHandler != null) {
                    oldHandler.uncaughtException(paramThread, paramThrowable);
                }
            }
        });
    }

    public void saveLogToFile(String text) {
        try {
            File logFile = new File("/storage/emulated/0/sampdata/files/samp_apk.log");
            
            // Cria pastas se não existirem
            if (!logFile.getParentFile().exists()) {
                logFile.getParentFile().mkdirs();
            }
            
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            writer.append("[").append(timeStamp).append("] ").append(text);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Erro ao salvar log: " + e.getMessage());
        }
    }

    // --- MÉTODOS NATIVOS E INTERFACE ---

    private native void initializeSAMP();
    public native void sendDialogResponse(int i, int i2, int i3, byte[] str);
    public native void onEventBackPressed();
    private native void onInputEnd(byte[] str);

    public static SAMP getInstance() {
        return instance;
    }

    // Permissões para Android 11+ (Xiaomi/Redmi Note 12)
    public void checkAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }
    }

    public void exitGame(){
        finishAndRemoveTask();
        System.exit(0);
    }

    public void showDialog(int dialogId, int dialogTypeId, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        final String caption = new String(bArr, StandardCharsets.UTF_8);
        final String content = new String(bArr2, StandardCharsets.UTF_8);
        final String leftBtnText = new String(bArr3, StandardCharsets.UTF_8);
        final String rightBtnText = new String(bArr4, StandardCharsets.UTF_8);
        runOnUiThread(() -> { 
            if(mDialog != null) this.mDialog.show(dialogId, dialogTypeId, caption, content, leftBtnText, rightBtnText); 
        });
    }

    // --- STUBS E UI (Implemente conforme necessário) ---
    private void showKeyboard() { runOnUiThread(() -> mKeyboard.ShowInputLayout()); }
    private void hideKeyboard() { runOnUiThread(() -> mKeyboard.HideInputLayout()); }
    private void hideLoadingScreen() { runOnUiThread(() -> mLoadingScreen.hide()); }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onEventBackPressed();
    }

    // Callbacks do Ciclo de Vida
    @Override protected void onStart() { super.onStart(); Log.i(TAG, "**** onStart"); }
    @Override protected void onResume() { super.onResume(); Log.i(TAG, "**** onResume"); }
    @Override protected void onPause() { super.onPause(); Log.i(TAG, "**** onPause"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.i(TAG, "**** onDestroy"); }
}
