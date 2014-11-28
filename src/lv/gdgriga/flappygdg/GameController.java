package lv.gdgriga.flappygdg;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.WakeLockOptions;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.modifier.MoveXModifier;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.scene.background.ParallaxBackground;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class GameController extends SimpleBaseGameActivity implements IOnSceneTouchListener {

    private final int CAMERA_WIDTH = 800;
    private final int CAMERA_HEIGHT = 480;

    private final int MAX_ATLAS_SIZE = 1024;

    private final float GDG_START_POS_X = 100;
    private final int GDG_Y_VELOCITY = 14;
    private final float GDG_SIZE = 50;

    private final int GRAVITY = 40;

    private final float OBSTACLE_SPAWN_DELAY = 1.4f;
    private final float OBSTACLE_MOVE_DURATION = 5.5f;
    private final int OBSTACLE_HEIGHT = 250;
    private final int OBSTACLE_WIDTH = 52;
    private final int OBSTACLE_OFFSET = OBSTACLE_HEIGHT / 2;

    private final Random random = new Random(System.currentTimeMillis());

    private ITextureRegion rigaRegion;
    private ITextureRegion gdgRegion;
    private ITextureRegion marginRegion;
    private ITextureRegion skyRegion;
    private TextureRegion obstacleRegion;

    private Body gdgBody;
    private Sprite gdg;

    private PhysicsWorld mPhysicsWorld;
    private PhysicsConnector pPhysicsConnector;

    private boolean isRunning = false;

    private Scene scene;
    private AutoParallaxBackground autoParallaxBackground;
    private TimerHandler timerHandler;

    private ArrayList<Sprite> obstacles = new ArrayList<Sprite>();

    @Override
    protected void onCreateResources() throws IOException {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
        BuildableBitmapTextureAtlas buildableBitmapTextureAtlas = new BuildableBitmapTextureAtlas(
                this.getTextureManager(), MAX_ATLAS_SIZE, MAX_ATLAS_SIZE, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        this.rigaRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(buildableBitmapTextureAtlas, this, "riga.png");

        BuildableBitmapTextureAtlas buildableBitmapTextureAtlas2 = new BuildableBitmapTextureAtlas(
                this.getTextureManager(), MAX_ATLAS_SIZE, MAX_ATLAS_SIZE, TextureOptions.BILINEAR);
        this.marginRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(buildableBitmapTextureAtlas2, this, "margins.png");
        this.gdgRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(buildableBitmapTextureAtlas2, this, "gdg.png");
        this.skyRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(buildableBitmapTextureAtlas2, this, "sky.png");
        this.obstacleRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(buildableBitmapTextureAtlas2, this, "obstacle.png");

        try {
            buildableBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(1, 1, 0));
            buildableBitmapTextureAtlas.load();
            buildableBitmapTextureAtlas2.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(1, 1, 0));
            buildableBitmapTextureAtlas2.load();
        } catch (ITextureAtlasBuilder.TextureAtlasBuilderException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Scene onCreateScene() {
        this.mEngine.registerUpdateHandler(new FPSLogger());
        return createGameScene();
    }

    private Scene createGameScene() {
        VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
        obstacles = new ArrayList<Sprite>();
        scene = new Scene() {
            @Override
            protected void onManagedUpdate(float pSecondsElapsed) {
                super.onManagedUpdate(pSecondsElapsed);
                if (gdg.getY() < marginRegion.getHeight() || gdg.getY() > CAMERA_HEIGHT - marginRegion.getHeight()) {
                    restartScene();
                }
                for (Sprite obstacle : obstacles) {
                    if (obstacle.collidesWith(gdg)) {
                        restartScene();
                    }
                    if (obstacle.getX() <= -gdgRegion.getWidth()) {
                        this.detachChild(obstacle);
                    }
                }
            }

            private void restartScene() {
                isRunning = false;
                autoParallaxBackground.setParallaxChangePerSecond(0);
                scene.unregisterUpdateHandler(timerHandler);
                scene.unregisterUpdateHandler(mPhysicsWorld);
                mEngine.setScene(createGameScene());
            }
        };
        timerHandler = new TimerHandler(OBSTACLE_SPAWN_DELAY, true, new ITimerCallback() {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler) {
                int i = random.nextInt(2);
                int verticalPos = OBSTACLE_OFFSET + (int) (marginRegion.getHeight());
                int rotation = 0;
                if (i == 0) {
                    verticalPos = CAMERA_HEIGHT - OBSTACLE_OFFSET - (int) (marginRegion.getHeight());
                    rotation = 180;
                }
                Sprite obstacle = new Sprite(CAMERA_WIDTH + obstacleRegion.getWidth() * 0.5f,
                        verticalPos, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, obstacleRegion, getVertexBufferObjectManager());
                obstacle.setRotation(rotation);
                scene.attachChild(obstacle);
                MoveXModifier moveXModifier = new MoveXModifier(OBSTACLE_MOVE_DURATION, obstacle.getX(), -OBSTACLE_WIDTH);
                obstacle.registerEntityModifier(moveXModifier);
                obstacles.add(obstacle);
            }
        });
        scene.setOnSceneTouchListener(this);
        scene.setBackgroundEnabled(true);
        scene.setBackground(new Background(1, 1, 0));
        autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 0);
        autoParallaxBackground.attachParallaxEntity(new ParallaxBackground.ParallaxEntity(0.0f, new Sprite(CAMERA_WIDTH * 0.5f, CAMERA_HEIGHT * 0.5f, this.skyRegion, vertexBufferObjectManager)));
        autoParallaxBackground.attachParallaxEntity(new ParallaxBackground.ParallaxEntity(-10.0f, new Sprite(CAMERA_WIDTH * 0.5f, CAMERA_HEIGHT - this.rigaRegion.getHeight() - 10, this.rigaRegion, vertexBufferObjectManager)));
        autoParallaxBackground.attachParallaxEntity(new ParallaxBackground.ParallaxEntity(-32f, new Sprite(CAMERA_WIDTH * 0.5f, this.marginRegion.getHeight() * 0.5f, this.marginRegion, vertexBufferObjectManager)));
        autoParallaxBackground.attachParallaxEntity(new ParallaxBackground.ParallaxEntity(-32f, new Sprite(CAMERA_WIDTH * 0.5f, CAMERA_HEIGHT - this.marginRegion.getHeight() * 0.5f, this.marginRegion, vertexBufferObjectManager)));
        scene.setBackground(autoParallaxBackground);

        gdg = new Sprite(GDG_START_POS_X, CAMERA_HEIGHT * 0.5f, GDG_SIZE, GDG_SIZE, gdgRegion, vertexBufferObjectManager);
        mPhysicsWorld = new PhysicsWorld(new Vector2(0, -GRAVITY), false);
        gdgBody = PhysicsFactory.createBoxBody(mPhysicsWorld, gdg, BodyDef.BodyType.DynamicBody, PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f));
        scene.attachChild(gdg);
        gdgBody.setLinearVelocity(new Vector2(gdgBody.getLinearVelocity().x, GDG_Y_VELOCITY));
        pPhysicsConnector = new PhysicsConnector(gdg, gdgBody, true, true);
        mPhysicsWorld.registerPhysicsConnector(pPhysicsConnector);
        return scene;
    }

    @Override
    public EngineOptions onCreateEngineOptions() {
        final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED,
                new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
        engineOptions.setWakeLockOptions(WakeLockOptions.SCREEN_ON);
        engineOptions.getTouchOptions().setNeedsMultiTouch(false);
        return engineOptions;
    }

    @Override
    public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
        if (pSceneTouchEvent.isActionDown() && isRunning) {
            gdgBody.setLinearVelocity(new Vector2(gdgBody.getLinearVelocity().x, GDG_Y_VELOCITY));
            return true;
        } else if (!isRunning) {
            autoParallaxBackground.setParallaxChangePerSecond(5);
            isRunning = true;
            scene.registerUpdateHandler(timerHandler);
            scene.registerUpdateHandler(mPhysicsWorld);
        }
        return false;
    }

}
