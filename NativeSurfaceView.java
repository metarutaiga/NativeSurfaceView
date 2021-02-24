/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.opengl;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.microedition.khronos.API.API10;
import javax.microedition.khronos.API.API11;
import javax.microedition.khronos.API.APIConfig;
import javax.microedition.khronos.API.APIContext;
import javax.microedition.khronos.API.APIDisplay;
import javax.microedition.khronos.API.APISurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

/**
 * An implementation of SurfaceView that uses the dedicated surface for
 * displaying OpenGL rendering.
 * <p>
 * A NativeSurfaceView provides the following features:
 * <p>
 * <ul>
 * <li>Manages a surface, which is a special piece of memory that can be
 * composited into the Android view system.
 * <li>Manages an API display, which enables OpenGL to render into a surface.
 * <li>Accepts a user-provided Renderer object that does the actual rendering.
 * <li>Renders on a dedicated thread to decouple rendering performance from the
 * UI thread.
 * <li>Supports both on-demand and continuous rendering.
 * <li>Optionally wraps, traces, and/or error-checks the renderer's OpenGL calls.
 * </ul>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about how to use OpenGL, read the
 * <a href="{@docRoot}guide/topics/graphics/opengl.html">OpenGL</a> developer guide.</p>
 * </div>
 *
 * <h3>Using NativeSurfaceView</h3>
 * <p>
 * Typically you use NativeSurfaceView by subclassing it and overriding one or more of the
 * View system input event methods. If your application does not need to override event
 * methods then NativeSurfaceView can be used as-is. For the most part
 * NativeSurfaceView behavior is customized by calling "set" methods rather than by subclassing.
 * For example, unlike a regular View, drawing is delegated to a separate Renderer object which
 * is registered with the NativeSurfaceView
 * using the {@link #setRenderer(Renderer)} call.
 * <p>
 * <h3>Initializing NativeSurfaceView</h3>
 * All you have to do to initialize a NativeSurfaceView is call {@link #setRenderer(Renderer)}.
 * However, if desired, you can modify the default behavior of NativeSurfaceView by calling one or
 * more of these methods before calling setRenderer:
 * <ul>
 * <li>{@link #setDebugFlags(int)}
 * <li>{@link #setAPIConfigChooser(boolean)}
 * <li>{@link #setAPIConfigChooser(APIConfigChooser)}
 * <li>{@link #setAPIConfigChooser(int, int, int, int, int, int)}
 * <li>{@link #setGLWrapper(GLWrapper)}
 * </ul>
 * <p>
 * <h4>Specifying the android.view.Surface</h4>
 * By default NativeSurfaceView will create a PixelFormat.RGB_888 format surface. If a translucent
 * surface is required, call getHolder().setFormat(PixelFormat.TRANSLUCENT).
 * The exact format of a TRANSLUCENT surface is device dependent, but it will be
 * a 32-bit-per-pixel surface with 8 bits per component.
 * <p>
 * <h4>Choosing an API Configuration</h4>
 * A given Android device may support multiple APIConfig rendering configurations.
 * The available configurations may differ in how many channels of data are present, as
 * well as how many bits are allocated to each channel. Therefore, the first thing
 * NativeSurfaceView has to do when starting to render is choose what APIConfig to use.
 * <p>
 * By default NativeSurfaceView chooses a APIConfig that has an RGB_888 pixel format,
 * with at least a 16-bit depth buffer and no stencil.
 * <p>
 * If you would prefer a different APIConfig
 * you can override the default behavior by calling one of the
 * setAPIConfigChooser methods.
 * <p>
 * <h4>Debug Behavior</h4>
 * You can optionally modify the behavior of NativeSurfaceView by calling
 * one or more of the debugging methods {@link #setDebugFlags(int)},
 * and {@link #setGLWrapper}. These methods may be called before and/or after setRenderer, but
 * typically they are called before setRenderer so that they take effect immediately.
 * <p>
 * <h4>Setting a Renderer</h4>
 * Finally, you must call {@link #setRenderer} to register a {@link Renderer}.
 * The renderer is
 * responsible for doing the actual OpenGL rendering.
 * <p>
 * <h3>Rendering Mode</h3>
 * Once the renderer is set, you can control whether the renderer draws
 * continuously or on-demand by calling
 * {@link #setRenderMode}. The default is continuous rendering.
 * <p>
 * <h3>Activity Life-cycle</h3>
 * A NativeSurfaceView must be notified when to pause and resume rendering. NativeSurfaceView clients
 * are required to call {@link #onPause()} when the activity stops and
 * {@link #onResume()} when the activity starts. These calls allow NativeSurfaceView to
 * pause and resume the rendering thread, and also allow NativeSurfaceView to release and recreate
 * the OpenGL display.
 * <p>
 * <h3>Handling events</h3>
 * <p>
 * To handle an event you will typically subclass NativeSurfaceView and override the
 * appropriate method, just as you would with any other View. However, when handling
 * the event, you may need to communicate with the Renderer object
 * that's running in the rendering thread. You can do this using any
 * standard Java cross-thread communication mechanism. In addition,
 * one relatively easy way to communicate with your renderer is
 * to call
 * {@link #queueEvent(Runnable)}. For example:
 * <pre class="prettyprint">
 * class MyNativeSurfaceView extends NativeSurfaceView {
 *
 *     private MyRenderer mMyRenderer;
 *
 *     public void start() {
 *         mMyRenderer = ...;
 *         setRenderer(mMyRenderer);
 *     }
 *
 *     public boolean onKeyDown(int keyCode, KeyEvent event) {
 *         if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
 *             queueEvent(new Runnable() {
 *                 // This method will be called on the rendering
 *                 // thread:
 *                 public void run() {
 *                     mMyRenderer.handleDpadCenter();
 *                 }});
 *             return true;
 *         }
 *         return super.onKeyDown(keyCode, event);
 *     }
 * }
 * </pre>
 *
 */
public class NativeSurfaceView extends SurfaceView implements SurfaceHolder.Callback2 {
    private final static String TAG = "NativeSurfaceView";
    private final static boolean LOG_ATTACH_DETACH = false;
    private final static boolean LOG_THREADS = false;
    private final static boolean LOG_PAUSE_RESUME = false;
    private final static boolean LOG_SURFACE = false;
    private final static boolean LOG_RENDERER = false;
    private final static boolean LOG_RENDERER_DRAW_FRAME = false;
    private final static boolean LOG_API = false;
    /**
     * The renderer only renders
     * when the surface is created, or when {@link #requestRender} is called.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     * @see #requestRender()
     */
    public final static int RENDERMODE_WHEN_DIRTY = 0;
    /**
     * The renderer is called
     * continuously to re-render the scene.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     */
    public final static int RENDERMODE_CONTINUOUSLY = 1;

    /**
     * Check glError() after every GL call and throw an exception if glError indicates
     * that an error has occurred. This can be used to help track down which OpenGL ES call
     * is causing an error.
     *
     * @see #getDebugFlags
     * @see #setDebugFlags
     */
    public final static int DEBUG_CHECK_GL_ERROR = 1;

    /**
     * Log GL calls to the system log at "verbose" level with tag "NativeSurfaceView".
     *
     * @see #getDebugFlags
     * @see #setDebugFlags
     */
    public final static int DEBUG_LOG_GL_CALLS = 2;

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public NativeSurfaceView(Context context) {
        super(context);
        init();
    }

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public NativeSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mRenderThread != null) {
                // RenderThread may still be running if this view was never
                // attached to a window.
                mRenderThread.requestExitAndWait();
            }
        } finally {
            super.finalize();
        }
    }

    private void init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // setFormat is done by SurfaceView in SDK 2.3 and newer. Uncomment
        // this statement if back-porting to 2.2 or older:
        // holder.setFormat(PixelFormat.RGB_565);
        //
        // setType is not needed for SDK 2.0 or newer. Uncomment this
        // statement if back-porting this code to older SDKs.
        // holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
    }

    /**
     * Set the glWrapper. If the glWrapper is not null, its
     * {@link GLWrapper#wrap(GL)} method is called
     * whenever a surface is created. A GLWrapper can be used to wrap
     * the GL object that's passed to the renderer. Wrapping a GL
     * object enables examining and modifying the behavior of the
     * GL calls made by the renderer.
     * <p>
     * Wrapping is typically used for debugging purposes.
     * <p>
     * The default value is null.
     * @param glWrapper the new GLWrapper
     */
    public void setGLWrapper(GLWrapper glWrapper) {
        mGLWrapper = glWrapper;
    }

    /**
     * Set the debug flags to a new value. The value is
     * constructed by OR-together zero or more
     * of the DEBUG_CHECK_* constants. The debug flags take effect
     * whenever a surface is created. The default value is zero.
     * @param debugFlags the new debug flags
     * @see #DEBUG_CHECK_GL_ERROR
     * @see #DEBUG_LOG_GL_CALLS
     */
    public void setDebugFlags(int debugFlags) {
        mDebugFlags = debugFlags;
    }

    /**
     * Get the current value of the debug flags.
     * @return the current value of the debug flags.
     */
    public int getDebugFlags() {
        return mDebugFlags;
    }

    /**
     * Control whether the API context is preserved when the NativeSurfaceView is paused and
     * resumed.
     * <p>
     * If set to true, then the API context may be preserved when the NativeSurfaceView is paused.
     * <p>
     * Prior to API level 11, whether the API context is actually preserved or not
     * depends upon whether the Android device can support an arbitrary number of
     * API contexts or not. Devices that can only support a limited number of API
     * contexts must release the API context in order to allow multiple applications
     * to share the GPU.
     * <p>
     * If set to false, the API context will be released when the NativeSurfaceView is paused,
     * and recreated when the NativeSurfaceView is resumed.
     * <p>
     *
     * The default is false.
     *
     * @param preserveOnPause preserve the API context when paused
     */
    public void setPreserveAPIContextOnPause(boolean preserveOnPause) {
        mPreserveAPIContextOnPause = preserveOnPause;
    }

    /**
     * @return true if the API context will be preserved when paused
     */
    public boolean getPreserveAPIContextOnPause() {
        return mPreserveAPIContextOnPause;
    }

    /**
     * Set the renderer associated with this view. Also starts the thread that
     * will call the renderer, which in turn causes the rendering to start.
     * <p>This method should be called once and only once in the life-cycle of
     * a NativeSurfaceView.
     * <p>The following NativeSurfaceView methods can only be called <em>before</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #setAPIConfigChooser(boolean)}
     * <li>{@link #setAPIConfigChooser(APIConfigChooser)}
     * <li>{@link #setAPIConfigChooser(int, int, int, int, int, int)}
     * </ul>
     * <p>
     * The following NativeSurfaceView methods can only be called <em>after</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #getRenderMode()}
     * <li>{@link #onPause()}
     * <li>{@link #onResume()}
     * <li>{@link #queueEvent(Runnable)}
     * <li>{@link #requestRender()}
     * <li>{@link #setRenderMode(int)}
     * </ul>
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    public void setRenderer(Renderer renderer) {
        checkRenderThreadState();
        if (mAPIConfigChooser == null) {
            mAPIConfigChooser = new SimpleAPIConfigChooser(true);
        }
        if (mAPIContextFactory == null) {
            mAPIContextFactory = new DefaultContextFactory();
        }
        if (mAPIWindowSurfaceFactory == null) {
            mAPIWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
        }
        mRenderer = renderer;
        mRenderThread = new RenderThread(mThisWeakRef);
        mRenderThread.start();
    }

    /**
     * Install a custom APIContextFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a context will be created with no shared context and
     * with a null attribute list.
     */
    public void setAPIContextFactory(APIContextFactory factory) {
        checkRenderThreadState();
        mAPIContextFactory = factory;
    }

    /**
     * Install a custom APIWindowSurfaceFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a window surface will be created with a null attribute list.
     */
    public void setAPIWindowSurfaceFactory(APIWindowSurfaceFactory factory) {
        checkRenderThreadState();
        mAPIWindowSurfaceFactory = factory;
    }

    /**
     * Install a custom APIConfigChooser.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setAPIConfigChooser method is called, then by default the
     * view will choose an APIConfig that is compatible with the current
     * android.view.Surface, with a depth buffer depth of
     * at least 16 bits.
     * @param configChooser
     */
    public void setAPIConfigChooser(APIConfigChooser configChooser) {
        checkRenderThreadState();
        mAPIConfigChooser = configChooser;
    }

    /**
     * Install a config chooser which will choose a config
     * as close to 16-bit RGB as possible, with or without an optional depth
     * buffer as close to 16-bits as possible.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setAPIConfigChooser method is called, then by default the
     * view will choose an RGB_888 surface with a depth buffer depth of
     * at least 16 bits.
     *
     * @param needDepth
     */
    public void setAPIConfigChooser(boolean needDepth) {
        setAPIConfigChooser(new SimpleAPIConfigChooser(needDepth));
    }

    /**
     * Install a config chooser which will choose a config
     * with at least the specified depthSize and stencilSize,
     * and exactly the specified redSize, greenSize, blueSize and alphaSize.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setAPIConfigChooser method is called, then by default the
     * view will choose an RGB_888 surface with a depth buffer depth of
     * at least 16 bits.
     *
     */
    public void setAPIConfigChooser(int redSize, int greenSize, int blueSize,
            int alphaSize, int depthSize, int stencilSize) {
        setAPIConfigChooser(new ComponentSizeChooser(redSize, greenSize,
                blueSize, alphaSize, depthSize, stencilSize));
    }

    /**
     * Inform the default APIContextFactory and default APIConfigChooser
     * which APIContext client version to pick.
     * <p>Use this method to create an OpenGL ES 2.0-compatible context.
     * Example:
     * <pre class="prettyprint">
     *     public MyView(Context context) {
     *         super(context);
     *         setAPIContextClientVersion(2); // Pick an OpenGL ES 2.0 context.
     *         setRenderer(new MyRenderer());
     *     }
     * </pre>
     * <p>Note: Activities which require OpenGL ES 2.0 should indicate this by
     * setting @lt;uses-feature android:glEsVersion="0x00020000" /> in the activity's
     * AndroidManifest.xml file.
     * <p>If this method is called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>This method only affects the behavior of the default APIContexFactory and the
     * default APIConfigChooser. If
     * {@link #setAPIContextFactory(APIContextFactory)} has been called, then the supplied
     * APIContextFactory is responsible for creating an OpenGL ES 2.0-compatible context.
     * If
     * {@link #setAPIConfigChooser(APIConfigChooser)} has been called, then the supplied
     * APIConfigChooser is responsible for choosing an OpenGL ES 2.0-compatible config.
     * @param version The APIContext client version to choose. Use 2 for OpenGL ES 2.0
     */
    public void setAPIContextClientVersion(int version) {
        checkRenderThreadState();
        mAPIContextClientVersion = version;
    }

    /**
     * Set the rendering mode. When renderMode is
     * RENDERMODE_CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When renderMode
     * is RENDERMODE_WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when {@link #requestRender} is called. Defaults to RENDERMODE_CONTINUOUSLY.
     * <p>
     * Using RENDERMODE_WHEN_DIRTY can improve battery life and overall system performance
     * by allowing the GPU and CPU to idle when the view does not need to be updated.
     * <p>
     * This method can only be called after {@link #setRenderer(Renderer)}
     *
     * @param renderMode one of the RENDERMODE_X constants
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     */
    public void setRenderMode(int renderMode) {
        mRenderThread.setRenderMode(renderMode);
    }

    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     * @return the current rendering mode.
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     */
    public int getRenderMode() {
        return mRenderThread.getRenderMode();
    }

    /**
     * Request that the renderer render a frame.
     * This method is typically used when the render mode has been set to
     * {@link #RENDERMODE_WHEN_DIRTY}, so that frames are only rendered on demand.
     * May be called
     * from any thread. Must not be called before a renderer has been set.
     */
    public void requestRender() {
        mRenderThread.requestRender();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of NativeSurfaceView.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        mRenderThread.surfaceCreated();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of NativeSurfaceView.
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return
        mRenderThread.surfaceDestroyed();
    }

    /**
     * This method is part of the SurfaceHolder.Callback interface, and is
     * not normally called or subclassed by clients of NativeSurfaceView.
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mRenderThread.onWindowResize(w, h);
    }

    /**
     * This method is part of the SurfaceHolder.Callback2 interface, and is
     * not normally called or subclassed by clients of NativeSurfaceView.
     */
    @Override
    public void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable finishDrawing) {
        if (mRenderThread != null) {
            mRenderThread.requestRenderAndNotify(finishDrawing);
        }
    }

    /**
     * This method is part of the SurfaceHolder.Callback2 interface, and is
     * not normally called or subclassed by clients of NativeSurfaceView.
     */
    @Deprecated
    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        // Since we are part of the framework we know only surfaceRedrawNeededAsync
        // will be called.
    }


    /**
     * Pause the rendering thread, optionally tearing down the API context
     * depending upon the value of {@link #setPreserveAPIContextOnPause(boolean)}.
     *
     * This method should be called when it is no longer desirable for the
     * NativeSurfaceView to continue rendering, such as in response to
     * {@link android.app.Activity#onStop Activity.onStop}.
     *
     * Must not be called before a renderer has been set.
     */
    public void onPause() {
        mRenderThread.onPause();
    }

    /**
     * Resumes the rendering thread, re-creating the OpenGL context if necessary. It
     * is the counterpart to {@link #onPause()}.
     *
     * This method should typically be called in
     * {@link android.app.Activity#onStart Activity.onStart}.
     *
     * Must not be called before a renderer has been set.
     */
    public void onResume() {
        mRenderThread.onResume();
    }

    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     * @param r the runnable to be run on the GL rendering thread.
     */
    public void queueEvent(Runnable r) {
        mRenderThread.queueEvent(r);
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of NativeSurfaceView.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onAttachedToWindow reattach =" + mDetached);
        }
        if (mDetached && (mRenderer != null)) {
            int renderMode = RENDERMODE_CONTINUOUSLY;
            if (mRenderThread != null) {
                renderMode = mRenderThread.getRenderMode();
            }
            mRenderThread = new RenderThread(mThisWeakRef);
            if (renderMode != RENDERMODE_CONTINUOUSLY) {
                mRenderThread.setRenderMode(renderMode);
            }
            mRenderThread.start();
        }
        mDetached = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onDetachedFromWindow");
        }
        if (mRenderThread != null) {
            mRenderThread.requestExitAndWait();
        }
        mDetached = true;
        super.onDetachedFromWindow();
    }

    // ----------------------------------------------------------------------

    /**
     * An interface used to wrap a GL interface.
     * <p>Typically
     * used for implementing debugging and tracing on top of the default
     * GL interface. You would typically use this by creating your own class
     * that implemented all the GL methods by delegating to another GL instance.
     * Then you could add your own behavior before or after calling the
     * delegate. All the GLWrapper would do was instantiate and return the
     * wrapper GL instance:
     * <pre class="prettyprint">
     * class MyGLWrapper implements GLWrapper {
     *     GL wrap(GL gl) {
     *         return new MyGLImplementation(gl);
     *     }
     *     static class MyGLImplementation implements GL,GL10,GL11,... {
     *         ...
     *     }
     * }
     * </pre>
     * @see #setGLWrapper(GLWrapper)
     */
    public interface GLWrapper {
        /**
         * Wraps a gl interface in another gl interface.
         * @param gl a GL interface that is to be wrapped.
         * @return either the input argument or another GL object that wraps the input argument.
         */
        GL wrap(GL gl);
    }

    /**
     * A generic renderer interface.
     * <p>
     * The renderer is responsible for making OpenGL calls to render a frame.
     * <p>
     * NativeSurfaceView clients typically create their own classes that implement
     * this interface, and then call {@link NativeSurfaceView#setRenderer} to
     * register the renderer with the NativeSurfaceView.
     * <p>
     *
     * <div class="special reference">
     * <h3>Developer Guides</h3>
     * <p>For more information about how to use OpenGL, read the
     * <a href="{@docRoot}guide/topics/graphics/opengl.html">OpenGL</a> developer guide.</p>
     * </div>
     *
     * <h3>Threading</h3>
     * The renderer will be called on a separate thread, so that rendering
     * performance is decoupled from the UI thread. Clients typically need to
     * communicate with the renderer from the UI thread, because that's where
     * input events are received. Clients can communicate using any of the
     * standard Java techniques for cross-thread communication, or they can
     * use the {@link NativeSurfaceView#queueEvent(Runnable)} convenience method.
     * <p>
     * <h3>API Context Lost</h3>
     * There are situations where the API rendering context will be lost. This
     * typically happens when device wakes up after going to sleep. When
     * the API context is lost, all OpenGL resources (such as textures) that are
     * associated with that context will be automatically deleted. In order to
     * keep rendering correctly, a renderer must recreate any lost resources
     * that it still needs. The {@link #onSurfaceCreated(GL10, APIConfig)} method
     * is a convenient place to do this.
     *
     *
     * @see #setRenderer(Renderer)
     */
    public interface Renderer {
        /**
         * Called when the surface is created or recreated.
         * <p>
         * Called when the rendering thread
         * starts and whenever the API context is lost. The API context will typically
         * be lost when the Android device awakes after going to sleep.
         * <p>
         * Since this method is called at the beginning of rendering, as well as
         * every time the API context is lost, this method is a convenient place to put
         * code to create resources that need to be created when the rendering
         * starts, and that need to be recreated when the API context is lost.
         * Textures are an example of a resource that you might want to create
         * here.
         * <p>
         * Note that when the API context is lost, all OpenGL resources associated
         * with that context will be automatically deleted. You do not need to call
         * the corresponding "glDelete" methods such as glDeleteTextures to
         * manually delete these lost resources.
         * <p>
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         * @param config the APIConfig of the created surface. Can be used
         * to create matching pbuffers.
         */
        void onSurfaceCreated(GL10 gl, APIConfig config);

        /**
         * Called when the surface changed size.
         * <p>
         * Called after the surface is created and whenever
         * the OpenGL ES surface size changes.
         * <p>
         * Typically you will set your viewport here. If your camera
         * is fixed then you could also set your projection matrix here:
         * <pre class="prettyprint">
         * void onSurfaceChanged(GL10 gl, int width, int height) {
         *     gl.glViewport(0, 0, width, height);
         *     // for a fixed camera, set the projection too
         *     float ratio = (float) width / height;
         *     gl.glMatrixMode(GL10.GL_PROJECTION);
         *     gl.glLoadIdentity();
         *     gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
         * }
         * </pre>
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         * @param width
         * @param height
         */
        void onSurfaceChanged(GL10 gl, int width, int height);

        /**
         * Called to draw the current frame.
         * <p>
         * This method is responsible for drawing the current frame.
         * <p>
         * The implementation of this method typically looks like this:
         * <pre class="prettyprint">
         * void onDrawFrame(GL10 gl) {
         *     gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
         *     //... other gl calls to render the scene ...
         * }
         * </pre>
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         */
        void onDrawFrame(GL10 gl);
    }

    /**
     * An interface for customizing the APICreateContext and APIDestroyContext calls.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link NativeSurfaceView#setAPIContextFactory(APIContextFactory)}
     */
    public interface APIContextFactory {
        APIContext createContext(API10 API, APIDisplay display, APIConfig APIConfig);
        void destroyContext(API10 API, APIDisplay display, APIContext context);
    }

    private class DefaultContextFactory implements APIContextFactory {
        private int API_CONTEXT_CLIENT_VERSION = 0x3098;

        public APIContext createContext(API10 API, APIDisplay display, APIConfig config) {
            int[] attrib_list = {API_CONTEXT_CLIENT_VERSION, mAPIContextClientVersion,
                    API10.API_NONE };

            return API.APICreateContext(display, config, API10.API_NO_CONTEXT,
                    mAPIContextClientVersion != 0 ? attrib_list : null);
        }

        public void destroyContext(API10 API, APIDisplay display,
                APIContext context) {
            if (!API.APIDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                if (LOG_THREADS) {
                    Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId());
                }
                APIHelper.throwAPIException("APIDestroyContex", API.APIGetError());
            }
        }
    }

    /**
     * An interface for customizing the APICreateWindowSurface and APIDestroySurface calls.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link NativeSurfaceView#setAPIWindowSurfaceFactory(APIWindowSurfaceFactory)}
     */
    public interface APIWindowSurfaceFactory {
        /**
         *  @return null if the surface cannot be constructed.
         */
        APISurface createWindowSurface(API10 API, APIDisplay display, APIConfig config,
                Object nativeWindow);
        void destroySurface(API10 API, APIDisplay display, APISurface surface);
    }

    private static class DefaultWindowSurfaceFactory implements APIWindowSurfaceFactory {

        public APISurface createWindowSurface(API10 API, APIDisplay display,
                APIConfig config, Object nativeWindow) {
            APISurface result = null;
            try {
                result = API.APICreateWindowSurface(display, config, nativeWindow, null);
            } catch (IllegalArgumentException e) {
                // This exception indicates that the surface flinger surface
                // is not valid. This can happen if the surface flinger surface has
                // been torn down, but the application has not yet been
                // notified via SurfaceHolder.Callback.surfaceDestroyed.
                // In theory the application should be notified first,
                // but in practice sometimes it is not. See b/4588890
                Log.e(TAG, "APICreateWindowSurface", e);
            }
            return result;
        }

        public void destroySurface(API10 API, APIDisplay display,
                APISurface surface) {
            API.APIDestroySurface(display, surface);
        }
    }

    /**
     * An interface for choosing an APIConfig configuration from a list of
     * potential configurations.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link NativeSurfaceView#setAPIConfigChooser(APIConfigChooser)}
     */
    public interface APIConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * {@link API10#APIChooseConfig} and iterating through the results. Please consult the
         * API specification available from The Khronos Group to learn how to call APIChooseConfig.
         * @param API the API10 for the current display.
         * @param display the current display.
         * @return the chosen configuration.
         */
        APIConfig chooseConfig(API10 API, APIDisplay display);
    }

    private abstract class BaseConfigChooser
            implements APIConfigChooser {
        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = filterConfigSpec(configSpec);
        }

        public APIConfig chooseConfig(API10 API, APIDisplay display) {
            int[] num_config = new int[1];
            if (!API.APIChooseConfig(display, mConfigSpec, null, 0,
                    num_config)) {
                throw new IllegalArgumentException("APIChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            APIConfig[] configs = new APIConfig[numConfigs];
            if (!API.APIChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("APIChooseConfig#2 failed");
            }
            APIConfig config = chooseConfig(API, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract APIConfig chooseConfig(API10 API, APIDisplay display,
                APIConfig[] configs);

        protected int[] mConfigSpec;

        private int[] filterConfigSpec(int[] configSpec) {
            if (mAPIContextClientVersion != 2 && mAPIContextClientVersion != 3) {
                return configSpec;
            }
            /* We know none of the subclasses define API_RENDERABLE_TYPE.
             * And we know the configSpec is well formed.
             */
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);
            newConfigSpec[len-1] = API10.API_RENDERABLE_TYPE;
            if (mAPIContextClientVersion == 2) {
                newConfigSpec[len] = API14.API_OPENGL_ES2_BIT;  /* API_OPENGL_ES2_BIT */
            } else {
                newConfigSpec[len] = APIExt.API_OPENGL_ES3_BIT_KHR; /* API_OPENGL_ES3_BIT_KHR */
            }
            newConfigSpec[len+1] = API10.API_NONE;
            return newConfigSpec;
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes,
     * and at least the specified depth and stencil sizes.
     */
    private class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                int alphaSize, int depthSize, int stencilSize) {
            super(new int[] {
                    API10.API_RED_SIZE, redSize,
                    API10.API_GREEN_SIZE, greenSize,
                    API10.API_BLUE_SIZE, blueSize,
                    API10.API_ALPHA_SIZE, alphaSize,
                    API10.API_DEPTH_SIZE, depthSize,
                    API10.API_STENCIL_SIZE, stencilSize,
                    API10.API_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
       }

        @Override
        public APIConfig chooseConfig(API10 API, APIDisplay display,
                APIConfig[] configs) {
            for (APIConfig config : configs) {
                int d = findConfigAttrib(API, display, config,
                        API10.API_DEPTH_SIZE, 0);
                int s = findConfigAttrib(API, display, config,
                        API10.API_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(API, display, config,
                            API10.API_RED_SIZE, 0);
                    int g = findConfigAttrib(API, display, config,
                             API10.API_GREEN_SIZE, 0);
                    int b = findConfigAttrib(API, display, config,
                              API10.API_BLUE_SIZE, 0);
                    int a = findConfigAttrib(API, display, config,
                            API10.API_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(API10 API, APIDisplay display,
                APIConfig config, int attribute, int defaultValue) {

            if (API.APIGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private int[] mValue;
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
        }

    /**
     * This class will choose a RGB_888 surface with
     * or without a depth buffer.
     *
     */
    private class SimpleAPIConfigChooser extends ComponentSizeChooser {
        public SimpleAPIConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    /**
     * An API helper class.
     */

    private static class APIHelper {
        public APIHelper(WeakReference<NativeSurfaceView> NativeSurfaceViewWeakRef) {
            mNativeSurfaceViewWeakRef = NativeSurfaceViewWeakRef;
        }

        /**
         * Initialize API for a given configuration spec.
         * @param configSpec
         */
        public void start() {
            if (LOG_API) {
                Log.w("APIHelper", "start() tid=" + Thread.currentThread().getId());
            }
            /*
             * Get an API instance
             */
            mAPI = (API10) APIContext.getAPI();

            /*
             * Get to the default display.
             */
            mAPIDisplay = mAPI.APIGetDisplay(API10.API_DEFAULT_DISPLAY);

            if (mAPIDisplay == API10.API_NO_DISPLAY) {
                throw new RuntimeException("APIGetDisplay failed");
            }

            /*
             * We can now initialize API for that display
             */
            int[] version = new int[2];
            if(!mAPI.APIInitialize(mAPIDisplay, version)) {
                throw new RuntimeException("APIInitialize failed");
            }
            NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
            if (view == null) {
                mAPIConfig = null;
                mAPIContext = null;
            } else {
                mAPIConfig = view.mAPIConfigChooser.chooseConfig(mAPI, mAPIDisplay);

                /*
                * Create an API context. We want to do this as rarely as we can, because an
                * API context is a somewhat heavy object.
                */
                mAPIContext = view.mAPIContextFactory.createContext(mAPI, mAPIDisplay, mAPIConfig);
            }
            if (mAPIContext == null || mAPIContext == API10.API_NO_CONTEXT) {
                mAPIContext = null;
                throwAPIException("createContext");
            }
            if (LOG_API) {
                Log.w("APIHelper", "createContext " + mAPIContext + " tid=" + Thread.currentThread().getId());
            }

            mAPISurface = null;
        }

        /**
         * Create an API surface for the current SurfaceHolder surface. If a surface
         * already exists, destroy it before creating the new surface.
         *
         * @return true if the surface was created successfully.
         */
        public boolean createSurface() {
            if (LOG_API) {
                Log.w("APIHelper", "createSurface()  tid=" + Thread.currentThread().getId());
            }
            /*
             * Check preconditions.
             */
            if (mAPI == null) {
                throw new RuntimeException("API not initialized");
            }
            if (mAPIDisplay == null) {
                throw new RuntimeException("APIDisplay not initialized");
            }
            if (mAPIConfig == null) {
                throw new RuntimeException("mAPIConfig not initialized");
            }

            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            destroySurfaceImp();

            /*
             * Create an API surface we can render into.
             */
            NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
            if (view != null) {
                mAPISurface = view.mAPIWindowSurfaceFactory.createWindowSurface(mAPI,
                        mAPIDisplay, mAPIConfig, view.getHolder());
            } else {
                mAPISurface = null;
            }

            if (mAPISurface == null || mAPISurface == API10.API_NO_SURFACE) {
                int error = mAPI.APIGetError();
                if (error == API10.API_BAD_NATIVE_WINDOW) {
                    Log.e("APIHelper", "createWindowSurface returned API_BAD_NATIVE_WINDOW.");
                }
                return false;
            }

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */
            if (!mAPI.APIMakeCurrent(mAPIDisplay, mAPISurface, mAPISurface, mAPIContext)) {
                /*
                 * Could not make the context current, probably because the underlying
                 * SurfaceView surface has been destroyed.
                 */
                logAPIErrorAsWarning("APIHelper", "APIMakeCurrent", mAPI.APIGetError());
                return false;
            }

            return true;
        }

        /**
         * Create a GL object for the current API context.
         * @return
         */
        GL creatAPI() {

            GL gl = mAPIContext.getGL();
            NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
            if (view != null) {
                if (view.mGLWrapper != null) {
                    gl = view.mGLWrapper.wrap(gl);
                }

                if ((view.mDebugFlags & (DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS)) != 0) {
                    int configFlags = 0;
                    Writer log = null;
                    if ((view.mDebugFlags & DEBUG_CHECK_GL_ERROR) != 0) {
                        configFlags |= GLDebugHelper.CONFIG_CHECK_GL_ERROR;
                    }
                    if ((view.mDebugFlags & DEBUG_LOG_GL_CALLS) != 0) {
                        log = new LogWriter();
                    }
                    gl = GLDebugHelper.wrap(gl, configFlags, log);
                }
            }
            return gl;
        }

        /**
         * Display the current render surface.
         * @return the API error code from APISwapBuffers.
         */
        public int swap() {
            if (! mAPI.APISwapBuffers(mAPIDisplay, mAPISurface)) {
                return mAPI.APIGetError();
            }
            return API10.API_SUCCESS;
        }

        public void destroySurface() {
            if (LOG_API) {
                Log.w("APIHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
            }
            destroySurfaceImp();
        }

        private void destroySurfaceImp() {
            if (mAPISurface != null && mAPISurface != API10.API_NO_SURFACE) {
                mAPI.APIMakeCurrent(mAPIDisplay, API10.API_NO_SURFACE,
                        API10.API_NO_SURFACE,
                        API10.API_NO_CONTEXT);
                NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mAPIWindowSurfaceFactory.destroySurface(mAPI, mAPIDisplay, mAPISurface);
                }
                mAPISurface = null;
            }
        }

        public void finish() {
            if (LOG_API) {
                Log.w("APIHelper", "finish() tid=" + Thread.currentThread().getId());
            }
            if (mAPIContext != null) {
                NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mAPIContextFactory.destroyContext(mAPI, mAPIDisplay, mAPIContext);
                }
                mAPIContext = null;
            }
            if (mAPIDisplay != null) {
                mAPI.APITerminate(mAPIDisplay);
                mAPIDisplay = null;
            }
        }

        private void throwAPIException(String function) {
            throwAPIException(function, mAPI.APIGetError());
        }

        public static void throwAPIException(String function, int error) {
            String message = formatAPIError(function, error);
            if (LOG_THREADS) {
                Log.e("APIHelper", "throwAPIException tid=" + Thread.currentThread().getId() + " "
                        + message);
            }
            throw new RuntimeException(message);
        }

        public static void logAPIErrorAsWarning(String tag, String function, int error) {
            Log.w(tag, formatAPIError(function, error));
        }

        public static String formatAPIError(String function, int error) {
            return function + " failed: " + APILogWrapper.getErrorString(error);
        }

        private WeakReference<NativeSurfaceView> mNativeSurfaceViewWeakRef;
        API10 mAPI;
        APIDisplay mAPIDisplay;
        APISurface mAPISurface;
        APIConfig mAPIConfig;
        @UnsupportedAppUsage
        APIContext mAPIContext;

    }

    /**
     * A generic GL Thread. Takes care of initializing API and GL. Delegates
     * to a Renderer instance to do the actual drawing. Can be configured to
     * render continuously or on request.
     *
     * All potentially blocking synchronization is done through the
     * sRenderThreadManager object. This avoids multiple-lock ordering issues.
     *
     */
    static class RenderThread extends Thread {
        RenderThread(WeakReference<NativeSurfaceView> NativeSurfaceViewWeakRef) {
            super();
            mWidth = 0;
            mHeight = 0;
            mRequestRender = true;
            mRenderMode = RENDERMODE_CONTINUOUSLY;
            mWantRenderNotification = false;
            mNativeSurfaceViewWeakRef = NativeSurfaceViewWeakRef;
        }

        @Override
        public void run() {
            setName("RenderThread " + getId());
            if (LOG_THREADS) {
                Log.i("RenderThread", "starting tid=" + getId());
            }

            try {
                guardedRun();
            } catch (InterruptedException e) {
                // fall thru and exit normally
            } finally {
                sRenderThreadManager.threadExiting(this);
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sRenderThreadManager) block.
         */
        private void stopAPISurfaceLocked() {
            if (mHaveAPISurface) {
                mHaveAPISurface = false;
                mAPIHelper.destroySurface();
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sRenderThreadManager) block.
         */
        private void stopAPIContextLocked() {
            if (mHaveAPIContext) {
                mAPIHelper.finish();
                mHaveAPIContext = false;
                sRenderThreadManager.releaseAPIContextLocked(this);
            }
        }
        private void guardedRun() throws InterruptedException {
            mAPIHelper = new APIHelper(mNativeSurfaceViewWeakRef);
            mHaveAPIContext = false;
            mHaveAPISurface = false;
            mWantRenderNotification = false;

            try {
                GL10 gl = null;
                boolean createAPIContext = false;
                boolean createAPISurface = false;
                boolean creatAPIInterface = false;
                boolean lostAPIContext = false;
                boolean sizeChanged = false;
                boolean wantRenderNotification = false;
                boolean doRenderNotification = false;
                boolean askedToReleaseAPIContext = false;
                int w = 0;
                int h = 0;
                Runnable event = null;
                Runnable finishDrawingRunnable = null;

                while (true) {
                    synchronized (sRenderThreadManager) {
                        while (true) {
                            if (mShouldExit) {
                                return;
                            }

                            if (! mEventQueue.isEmpty()) {
                                event = mEventQueue.remove(0);
                                break;
                            }

                            // Update the pause state.
                            boolean pausing = false;
                            if (mPaused != mRequestPaused) {
                                pausing = mRequestPaused;
                                mPaused = mRequestPaused;
                                sRenderThreadManager.notifyAll();
                                if (LOG_PAUSE_RESUME) {
                                    Log.i("RenderThread", "mPaused is now " + mPaused + " tid=" + getId());
                                }
                            }

                            // Do we need to give up the API context?
                            if (mShouldReleaseAPIContext) {
                                if (LOG_SURFACE) {
                                    Log.i("RenderThread", "releasing API context because asked to tid=" + getId());
                                }
                                stopAPISurfaceLocked();
                                stopAPIContextLocked();
                                mShouldReleaseAPIContext = false;
                                askedToReleaseAPIContext = true;
                            }

                            // Have we lost the API context?
                            if (lostAPIContext) {
                                stopAPISurfaceLocked();
                                stopAPIContextLocked();
                                lostAPIContext = false;
                            }

                            // When pausing, release the API surface:
                            if (pausing && mHaveAPISurface) {
                                if (LOG_SURFACE) {
                                    Log.i("RenderThread", "releasing API surface because paused tid=" + getId());
                                }
                                stopAPISurfaceLocked();
                            }

                            // When pausing, optionally release the API Context:
                            if (pausing && mHaveAPIContext) {
                                NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
                                boolean preserveAPIContextOnPause = view == null ?
                                        false : view.mPreserveAPIContextOnPause;
                                if (!preserveAPIContextOnPause) {
                                    stopAPIContextLocked();
                                    if (LOG_SURFACE) {
                                        Log.i("RenderThread", "releasing API context because paused tid=" + getId());
                                    }
                                }
                            }

                            // Have we lost the SurfaceView surface?
                            if ((! mHasSurface) && (! mWaitingForSurface)) {
                                if (LOG_SURFACE) {
                                    Log.i("RenderThread", "noticed surfaceView surface lost tid=" + getId());
                                }
                                if (mHaveAPISurface) {
                                    stopAPISurfaceLocked();
                                }
                                mWaitingForSurface = true;
                                mSurfaceIsBad = false;
                                sRenderThreadManager.notifyAll();
                            }

                            // Have we acquired the surface view surface?
                            if (mHasSurface && mWaitingForSurface) {
                                if (LOG_SURFACE) {
                                    Log.i("RenderThread", "noticed surfaceView surface acquired tid=" + getId());
                                }
                                mWaitingForSurface = false;
                                sRenderThreadManager.notifyAll();
                            }

                            if (doRenderNotification) {
                                if (LOG_SURFACE) {
                                    Log.i("RenderThread", "sending render notification tid=" + getId());
                                }
                                mWantRenderNotification = false;
                                doRenderNotification = false;
                                mRenderComplete = true;
                                sRenderThreadManager.notifyAll();
                            }

                            if (mFinishDrawingRunnable != null) {
                                finishDrawingRunnable = mFinishDrawingRunnable;
                                mFinishDrawingRunnable = null;
                            }

                            // Ready to draw?
                            if (readyToDraw()) {

                                // If we don't have an API context, try to acquire one.
                                if (! mHaveAPIContext) {
                                    if (askedToReleaseAPIContext) {
                                        askedToReleaseAPIContext = false;
                                    } else {
                                        try {
                                            mAPIHelper.start();
                                        } catch (RuntimeException t) {
                                            sRenderThreadManager.releaseAPIContextLocked(this);
                                            throw t;
                                        }
                                        mHaveAPIContext = true;
                                        createAPIContext = true;

                                        sRenderThreadManager.notifyAll();
                                    }
                                }

                                if (mHaveAPIContext && !mHaveAPISurface) {
                                    mHaveAPISurface = true;
                                    createAPISurface = true;
                                    creatAPIInterface = true;
                                    sizeChanged = true;
                                }

                                if (mHaveAPISurface) {
                                    if (mSizeChanged) {
                                        sizeChanged = true;
                                        w = mWidth;
                                        h = mHeight;
                                        mWantRenderNotification = true;
                                        if (LOG_SURFACE) {
                                            Log.i("RenderThread",
                                                    "noticing that we want render notification tid="
                                                    + getId());
                                        }

                                        // Destroy and recreate the API surface.
                                        createAPISurface = true;

                                        mSizeChanged = false;
                                    }
                                    mRequestRender = false;
                                    sRenderThreadManager.notifyAll();
                                    if (mWantRenderNotification) {
                                        wantRenderNotification = true;
                                    }
                                    break;
                                }
                            } else {
                                if (finishDrawingRunnable != null) {
                                    Log.w(TAG, "Warning, !readyToDraw() but waiting for " +
                                            "draw finished! Early reporting draw finished.");
                                    finishDrawingRunnable.run();
                                    finishDrawingRunnable = null;
                                }
                            }
                            // By design, this is the only place in a RenderThread thread where we wait().
                            if (LOG_THREADS) {
                                Log.i("RenderThread", "waiting tid=" + getId()
                                    + " mHaveAPIContext: " + mHaveAPIContext
                                    + " mHaveAPISurface: " + mHaveAPISurface
                                    + " mFinishedCreatingAPISurface: " + mFinishedCreatingAPISurface
                                    + " mPaused: " + mPaused
                                    + " mHasSurface: " + mHasSurface
                                    + " mSurfaceIsBad: " + mSurfaceIsBad
                                    + " mWaitingForSurface: " + mWaitingForSurface
                                    + " mWidth: " + mWidth
                                    + " mHeight: " + mHeight
                                    + " mRequestRender: " + mRequestRender
                                    + " mRenderMode: " + mRenderMode);
                            }
                            sRenderThreadManager.wait();
                        }
                    } // end of synchronized(sRenderThreadManager)

                    if (event != null) {
                        event.run();
                        event = null;
                        continue;
                    }

                    if (createAPISurface) {
                        if (LOG_SURFACE) {
                            Log.w("RenderThread", "API createSurface");
                        }
                        if (mAPIHelper.createSurface()) {
                            synchronized(sRenderThreadManager) {
                                mFinishedCreatingAPISurface = true;
                                sRenderThreadManager.notifyAll();
                            }
                        } else {
                            synchronized(sRenderThreadManager) {
                                mFinishedCreatingAPISurface = true;
                                mSurfaceIsBad = true;
                                sRenderThreadManager.notifyAll();
                            }
                            continue;
                        }
                        createAPISurface = false;
                    }

                    if (creatAPIInterface) {
                        gl = (GL10) mAPIHelper.creatAPI();

                        creatAPIInterface = false;
                    }

                    if (createAPIContext) {
                        if (LOG_RENDERER) {
                            Log.w("RenderThread", "onSurfaceCreated");
                        }
                        NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onSurfaceCreated");
                                view.mRenderer.onSurfaceCreated(gl, mAPIHelper.mAPIConfig);
                            } finally {
                                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
                            }
                        }
                        createAPIContext = false;
                    }

                    if (sizeChanged) {
                        if (LOG_RENDERER) {
                            Log.w("RenderThread", "onSurfaceChanged(" + w + ", " + h + ")");
                        }
                        NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onSurfaceChanged");
                                view.mRenderer.onSurfaceChanged(gl, w, h);
                            } finally {
                                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
                            }
                        }
                        sizeChanged = false;
                    }

                    if (LOG_RENDERER_DRAW_FRAME) {
                        Log.w("RenderThread", "onDrawFrame tid=" + getId());
                    }
                    {
                        NativeSurfaceView view = mNativeSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                Trace.traceBegin(Trace.TRACE_TAG_VIEW, "onDrawFrame");
                                view.mRenderer.onDrawFrame(gl);
                                if (finishDrawingRunnable != null) {
                                    finishDrawingRunnable.run();
                                    finishDrawingRunnable = null;
                                }
                            } finally {
                                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
                            }
                        }
                    }
                    int swapError = mAPIHelper.swap();
                    switch (swapError) {
                        case API10.API_SUCCESS:
                            break;
                        case API11.API_CONTEXT_LOST:
                            if (LOG_SURFACE) {
                                Log.i("RenderThread", "API context lost tid=" + getId());
                            }
                            lostAPIContext = true;
                            break;
                        default:
                            // Other errors typically mean that the current surface is bad,
                            // probably because the SurfaceView surface has been destroyed,
                            // but we haven't been notified yet.
                            // Log the error to help developers understand why rendering stopped.
                            APIHelper.logAPIErrorAsWarning("RenderThread", "APISwapBuffers", swapError);

                            synchronized(sRenderThreadManager) {
                                mSurfaceIsBad = true;
                                sRenderThreadManager.notifyAll();
                            }
                            break;
                    }

                    if (wantRenderNotification) {
                        doRenderNotification = true;
                        wantRenderNotification = false;
                    }
                }

            } finally {
                /*
                 * clean-up everything...
                 */
                synchronized (sRenderThreadManager) {
                    stopAPISurfaceLocked();
                    stopAPIContextLocked();
                }
            }
        }

        public boolean ableToDraw() {
            return mHaveAPIContext && mHaveAPISurface && readyToDraw();
        }

        private boolean readyToDraw() {
            return (!mPaused) && mHasSurface && (!mSurfaceIsBad)
                && (mWidth > 0) && (mHeight > 0)
                && (mRequestRender || (mRenderMode == RENDERMODE_CONTINUOUSLY));
        }

        public void setRenderMode(int renderMode) {
            if ( !((RENDERMODE_WHEN_DIRTY <= renderMode) && (renderMode <= RENDERMODE_CONTINUOUSLY)) ) {
                throw new IllegalArgumentException("renderMode");
            }
            synchronized(sRenderThreadManager) {
                mRenderMode = renderMode;
                sRenderThreadManager.notifyAll();
            }
        }

        public int getRenderMode() {
            synchronized(sRenderThreadManager) {
                return mRenderMode;
            }
        }

        public void requestRender() {
            synchronized(sRenderThreadManager) {
                mRequestRender = true;
                sRenderThreadManager.notifyAll();
            }
        }

        public void requestRenderAndNotify(Runnable finishDrawing) {
            synchronized(sRenderThreadManager) {
                // If we are already on the GL thread, this means a client callback
                // has caused reentrancy, for example via updating the SurfaceView parameters.
                // We will return to the client rendering code, so here we don't need to
                // do anything.
                if (Thread.currentThread() == this) {
                    return;
                }

                mWantRenderNotification = true;
                mRequestRender = true;
                mRenderComplete = false;
                mFinishDrawingRunnable = finishDrawing;

                sRenderThreadManager.notifyAll();
            }
        }

        public void surfaceCreated() {
            synchronized(sRenderThreadManager) {
                if (LOG_THREADS) {
                    Log.i("RenderThread", "surfaceCreated tid=" + getId());
                }
                mHasSurface = true;
                mFinishedCreatingAPISurface = false;
                sRenderThreadManager.notifyAll();
                while (mWaitingForSurface
                       && !mFinishedCreatingAPISurface
                       && !mExited) {
                    try {
                        sRenderThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void surfaceDestroyed() {
            synchronized(sRenderThreadManager) {
                if (LOG_THREADS) {
                    Log.i("RenderThread", "surfaceDestroyed tid=" + getId());
                }
                mHasSurface = false;
                sRenderThreadManager.notifyAll();
                while((!mWaitingForSurface) && (!mExited)) {
                    try {
                        sRenderThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onPause() {
            synchronized (sRenderThreadManager) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("RenderThread", "onPause tid=" + getId());
                }
                mRequestPaused = true;
                sRenderThreadManager.notifyAll();
                while ((! mExited) && (! mPaused)) {
                    if (LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onPause waiting for mPaused.");
                    }
                    try {
                        sRenderThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onResume() {
            synchronized (sRenderThreadManager) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("RenderThread", "onResume tid=" + getId());
                }
                mRequestPaused = false;
                mRequestRender = true;
                mRenderComplete = false;
                sRenderThreadManager.notifyAll();
                while ((! mExited) && mPaused && (!mRenderComplete)) {
                    if (LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onResume waiting for !mPaused.");
                    }
                    try {
                        sRenderThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onWindowResize(int w, int h) {
            synchronized (sRenderThreadManager) {
                mWidth = w;
                mHeight = h;
                mSizeChanged = true;
                mRequestRender = true;
                mRenderComplete = false;

                // If we are already on the GL thread, this means a client callback
                // has caused reentrancy, for example via updating the SurfaceView parameters.
                // We need to process the size change eventually though and update our APISurface.
                // So we set the parameters and return so they can be processed on our
                // next iteration.
                if (Thread.currentThread() == this) {
                    return;
                }

                sRenderThreadManager.notifyAll();

                // Wait for thread to react to resize and render a frame
                while (! mExited && !mPaused && !mRenderComplete
                        && ableToDraw()) {
                    if (LOG_SURFACE) {
                        Log.i("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
                    }
                    try {
                        sRenderThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestExitAndWait() {
            // don't call this from RenderThread thread or it is a guaranteed
            // deadlock!
            synchronized(sRenderThreadManager) {
                mShouldExit = true;
                sRenderThreadManager.notifyAll();
                while (! mExited) {
                    try {
                        sRenderThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestReleaseAPIContextLocked() {
            mShouldReleaseAPIContext = true;
            sRenderThreadManager.notifyAll();
        }

        /**
         * Queue an "event" to be run on the GL rendering thread.
         * @param r the runnable to be run on the GL rendering thread.
         */
        public void queueEvent(Runnable r) {
            if (r == null) {
                throw new IllegalArgumentException("r must not be null");
            }
            synchronized(sRenderThreadManager) {
                mEventQueue.add(r);
                sRenderThreadManager.notifyAll();
            }
        }

        // Once the thread is started, all accesses to the following member
        // variables are protected by the sRenderThreadManager monitor
        private boolean mShouldExit;
        private boolean mExited;
        private boolean mRequestPaused;
        private boolean mPaused;
        private boolean mHasSurface;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private boolean mHaveAPIContext;
        private boolean mHaveAPISurface;
        private boolean mFinishedCreatingAPISurface;
        private boolean mShouldReleaseAPIContext;
        private int mWidth;
        private int mHeight;
        private int mRenderMode;
        private boolean mRequestRender;
        private boolean mWantRenderNotification;
        private boolean mRenderComplete;
        private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
        private boolean mSizeChanged = true;
        private Runnable mFinishDrawingRunnable = null;

        // End of member variables protected by the sRenderThreadManager monitor.

        @UnsupportedAppUsage
        private APIHelper mAPIHelper;

        /**
         * Set once at thread construction time, nulled out when the parent view is garbage
         * called. This weak reference allows the NativeSurfaceView to be garbage collected while
         * the RenderThread is still alive.
         */
        private WeakReference<NativeSurfaceView> mNativeSurfaceViewWeakRef;

    }

    static class LogWriter extends Writer {

        @Override public void close() {
            flushBuilder();
        }

        @Override public void flush() {
            flushBuilder();
        }

        @Override public void write(char[] buf, int offset, int count) {
            for(int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if ( c == '\n') {
                    flushBuilder();
                }
                else {
                    mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (mBuilder.length() > 0) {
                Log.v("NativeSurfaceView", mBuilder.toString());
                mBuilder.delete(0, mBuilder.length());
            }
        }

        private StringBuilder mBuilder = new StringBuilder();
    }


    private void checkRenderThreadState() {
        if (mRenderThread != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
    }

    private static class RenderThreadManager {
        private static String TAG = "RenderThreadManager";

        public synchronized void threadExiting(RenderThread thread) {
            if (LOG_THREADS) {
                Log.i("RenderThread", "exiting tid=" +  thread.getId());
            }
            thread.mExited = true;
            notifyAll();
        }

        /*
         * Releases the API context. Requires that we are already in the
         * sRenderThreadManager monitor when this is called.
         */
        public void releaseAPIContextLocked(RenderThread thread) {
            notifyAll();
        }
    }

    private static final RenderThreadManager sRenderThreadManager = new RenderThreadManager();

    private final WeakReference<NativeSurfaceView> mThisWeakRef =
            new WeakReference<NativeSurfaceView>(this);
    @UnsupportedAppUsage
    private RenderThread mRenderThread;
    @UnsupportedAppUsage
    private Renderer mRenderer;
    private boolean mDetached;
    private APIConfigChooser mAPIConfigChooser;
    private APIContextFactory mAPIContextFactory;
    private APIWindowSurfaceFactory mAPIWindowSurfaceFactory;
    private GLWrapper mGLWrapper;
    private int mDebugFlags;
    private int mAPIContextClientVersion;
    private boolean mPreserveAPIContextOnPause;
}
