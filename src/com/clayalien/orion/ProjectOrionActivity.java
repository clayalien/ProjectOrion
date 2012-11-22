package com.clayalien.orion;

import java.util.ArrayList;

import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.extension.tmx.TMXLayer;
import org.andengine.extension.tmx.TMXLoader;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.extension.tmx.TMXProperties;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.extension.tmx.TMXTileProperty;
import org.andengine.extension.tmx.TMXTiledMap;
import org.andengine.extension.tmx.util.exception.TMXLoadException;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.debug.Debug;
import org.andengine.util.math.MathUtils;

import android.opengl.GLES20;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class ProjectOrionActivity extends SimpleBaseGameActivity {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 480;
	private static final int CAMERA_HEIGHT = 320;
	
	// ===========================================================
	// Fields
	// ===========================================================

	private BoundCamera mBoundChaseCamera;

	private BitmapTextureAtlas mBitmapTextureAtlas;
	private TMXTiledMap mTMXTiledMap;
	private TiledTextureRegion mFaceTextureRegion;
	private BitmapTextureAtlas mOnScreenControlTexture;
	private TextureRegion mOnScreenControlBaseTextureRegion;
	private TextureRegion mOnScreenControlKnobTextureRegion;
	private PhysicsWorld mPhysicsWorld;
	private Body mCarBody;
	private AnimatedSprite mCar;

    @Override
	public EngineOptions onCreateEngineOptions() {
		Toast.makeText(this, "Welcome to Orion City.", Toast.LENGTH_LONG).show();

		//This loads up the area of the map to show
		this.mBoundChaseCamera = new BoundCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mBoundChaseCamera);
	}

    @Override
	public void onCreateResources() {
    	
    	//Loads the player character
    	//Load the path for graphics
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		

		//Create player character
		this.mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 235, 159, TextureOptions.BILINEAR);
		this.mFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "spacecraft.png", 0, 0, 5, 3);
		this.mBitmapTextureAtlas.load();

		//Create the controls
		this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_knob.png", 128, 0);
		this.mOnScreenControlTexture.load();
	}

    @Override
	public Scene onCreateScene() {
    	
		this.mEngine.registerUpdateHandler(new FPSLogger());
		
		this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, 0), true, 1, 1);
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();

		final Scene scene = new Scene();

		//Load the TMX map
		try {
			final TMXLoader tmxLoader = new TMXLoader(this.getAssets(), this.mEngine.getTextureManager(), TextureOptions.BILINEAR_PREMULTIPLYALPHA, this.getVertexBufferObjectManager(), new ITMXTilePropertiesListener() {
				@Override
				public void onTMXTileWithPropertiesCreated(final TMXTiledMap pTMXTiledMap, final TMXLayer pTMXLayer, final TMXTile pTMXTile, final TMXProperties<TMXTileProperty> pTMXTileProperties) {
					/* We are going to count the tiles that have the property "cactus=true" set. */
					if(pTMXTileProperties.containsTMXProperty("solid", "true")) {
						final Rectangle wall = new Rectangle(pTMXTile.getTileX(), pTMXTile.getTileY(), pTMXTile.getTileWidth(), pTMXTile.getTileHeight(), vertexBufferObjectManager);

						final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
						PhysicsFactory.createBoxBody(ProjectOrionActivity.this.mPhysicsWorld, wall, BodyType.StaticBody, wallFixtureDef);
					}
					
				}
			});
			this.mTMXTiledMap = tmxLoader.loadFromAsset("tmx/city.tmx");
			

		} catch (final TMXLoadException e) {
			Debug.e(e);
		}

		final ArrayList<TMXLayer> tmxLayers = this.mTMXTiledMap.getTMXLayers();
		final int mapHeight = tmxLayers.get(0).getHeight();
		final int mapWidth = tmxLayers.get(0).getWidth();
		/* Make the camera not exceed the bounds of the TMXEntity. */
		this.mBoundChaseCamera.setBounds(0, 0, mapHeight, mapWidth);
		this.mBoundChaseCamera.setBoundsEnabled(true);
		
		//Load every layer but the last one (meta data)
		for (int layerID = 0; layerID < tmxLayers.size()-1; layerID++) {
		    scene.attachChild(tmxLayers.get(layerID));
		}

		this.mCar = new AnimatedSprite(5*32, 8*32, this.mFaceTextureRegion, vertexBufferObjectManager);
		this.mCar.animate(new long[]{100, 100, 100, 100}, 1, 4, true);

		final FixtureDef carFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);
		this.mCarBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, this.mCar, BodyType.DynamicBody, carFixtureDef);

		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this.mCar, this.mCarBody, true, false));


		final AnalogOnScreenControl analogOnScreenControl = new AnalogOnScreenControl(0, CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight(), this.mBoundChaseCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, 200, this.getVertexBufferObjectManager(), new IAnalogOnScreenControlListener() {
			@Override
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {
				final Body carBody = ProjectOrionActivity.this.mCarBody;

				final Vector2 velocity = Vector2Pool.obtain(pValueX * 8, pValueY * 8);
				carBody.setLinearVelocity(velocity);
				Vector2Pool.recycle(velocity);

				if (pValueX != 0 || pValueY !=0 ) {
					final float rotationInRad = (float)Math.atan2(-pValueX, pValueY);
					carBody.setTransform(carBody.getWorldCenter(), rotationInRad);
	
					ProjectOrionActivity.this.mCar.setRotation(MathUtils.radToDeg(rotationInRad));
				} 
			}

			@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
				/* Nothing. */
			}
		});
		analogOnScreenControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		analogOnScreenControl.getControlBase().setAlpha(0.5f);
		analogOnScreenControl.getControlBase().setScaleCenter(0, 128);
		analogOnScreenControl.getControlBase().setScale(1.25f);
		analogOnScreenControl.getControlKnob().setScale(1.25f);
		analogOnScreenControl.refreshControlKnobPosition();
		

		scene.setChildScene(analogOnScreenControl);
		
		this.mBoundChaseCamera.setChaseEntity(this.mCar);

		final Rectangle bottomOuter = new Rectangle(0, mapHeight - 2, mapWidth, 2, vertexBufferObjectManager);
		final Rectangle topOuter = new Rectangle(0, 0, mapWidth, 2, vertexBufferObjectManager);
		final Rectangle leftOuter = new Rectangle(0, 0, 2, mapHeight, vertexBufferObjectManager);
		final Rectangle rightOuter = new Rectangle(mapWidth - 2, 0, 2, mapHeight, vertexBufferObjectManager);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, bottomOuter, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, topOuter, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, leftOuter, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, rightOuter, BodyType.StaticBody, wallFixtureDef);

		scene.attachChild(this.mCar);

		scene.registerUpdateHandler(this.mPhysicsWorld);
		return scene;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}