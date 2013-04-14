package au.com.ds.ef.ae.AtmEmulator;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * User: andrey
 * Date: 27/03/13
 * Time: 3:00 PM
 */
public class UiThreadExecutor implements Executor {
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(Runnable command) {
        handler.post(command);
    }
}
