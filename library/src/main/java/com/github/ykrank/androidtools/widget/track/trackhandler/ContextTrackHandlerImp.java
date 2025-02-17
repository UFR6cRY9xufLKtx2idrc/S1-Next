package com.github.ykrank.androidtools.widget.track.trackhandler;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.github.ykrank.androidtools.widget.track.TrackAgent;

import org.jetbrains.annotations.NotNull;

/**
 * Created by ykrank on 2016/12/27.
 */

public abstract class ContextTrackHandlerImp<T> implements TrackHandler<T> {
    protected TrackAgent agent;

    protected ContextTrackHandlerImp(@NonNull TrackAgent agent) {
        this.agent = agent;
    }

    @Override
    public final void track(@NotNull Handler handler, T event) {
        trackEvent(event);
    }

    /**
     * event handle action. run on UI thread.
     *
     * @param event Event
     * @return does action success
     */
    @UiThread
    public abstract boolean trackEvent(T event);
}
