package box2dLight;

/**
 * @author kalle_h
 *
 */

import shaders.LightShader;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class RayHandler {

	final static int MIN_RAYS = 3;
	boolean isGL20;

	boolean culling = true;
	boolean shadows = true;
	boolean blur = true;
	int blurNum = 1;
	float ambientLight = 0.0f;

	int MAX_RAYS;
	World world;

	OrthographicCamera camera;
	ShaderProgram lightShader;

	private GL20 gl20;
	private Mesh box;

	final public Array<Light> lightList = new Array<Light>(
			false, 16,
			Light.class);

	/**
	 * Construct handler that manages everything related to updating and
	 * rendering the lights MINIMUM parameters needed are world where collision
	 * geometry is taken and OrthographicCamera. Camera will not be modified in
	 * anyway but used only for culling, directional lights and rendering lights
	 * to proper places. Camera must be set to work with box2d coordinates.
	 * Default setting: culling = true, shadows = true, blur =
	 * true(GL2.0),blurNum = 1, ambientLight = 0.0f;
	 * 
	 * NOTE1: rays number per lights are capped to 1024. For different size use
	 * other constructor
	 * 
	 * NOTE2: On GL 2.0 FBO size is 1/4 * screen size and used by default. For
	 * different sizes use other constructor
	 * 
	 * @param world
	 * @param camera
	 */
	public RayHandler(World world, OrthographicCamera camera) {
		this(world, camera, defaultMaximum, Gdx.graphics
				.getWidth() / 4, Gdx.graphics.getHeight() / 4);
	}

	/**
	 * Construct handler that manages everything related to updating and
	 * rendering the lights MINIMUM parameters needed are world where collision
	 * geometry is taken and OrthographicCamera. Camera will not be modified in
	 * anyway but used only for culling, directional lights and rendering lights
	 * to proper places. Camera must be set to work with box2d coordinates.
	 * Default setting: culling = true, shadows = true, blur =
	 * true(GL2.0),blurNum = 1, ambientLight = 0.0f;
	 * 
	 * @param world
	 * @param camera
	 * @param maxRayCount
	 * @param fboWidth
	 * @param fboHeigth
	 */
	public RayHandler(World world, OrthographicCamera camera, int maxRayCount,
			int fboWidth,
			int fboHeigth) {
		this.world = world;
		this.camera = camera;
		updateCameraCorners();
		MAX_RAYS = maxRayCount < MIN_RAYS ? MIN_RAYS : maxRayCount;

		m_segments = new float[maxRayCount * 8];
		m_x = new float[maxRayCount];
		m_y = new float[maxRayCount];
		m_f = new float[maxRayCount];

		isGL20 = Gdx.graphics.isGL20Available();
		if (isGL20) {

			lightMap = new LightMap(this, fboWidth, fboHeigth);
			lightShader = LightShader.createLightShader();

			gl20 = Gdx.graphics.getGL20();
		} else {
			gl10 = Gdx.graphics.getGL10();

			box = new Mesh(true, 12, 0, new VertexAttribute(Usage.Position,
						2,
						"vertex_positions"), new VertexAttribute(
						Usage.ColorPacked,
						4, "quad_colors"));
			setShadowBox();

		}

	}

	public void enableCulling() {
		culling = true;
	}

	public void disableCulling() {
		culling = false;
	}

	boolean intersect(float x, float y, float side) {
		final float bx = x - side;
		final float bx2 = x + side;
		final float by = y - side;
		final float by2 = y + side;
		return (x1 < bx2 && x2 > bx && y1 < by2 && y2 > by);
	}

	void updateCameraCorners() {
		updateCameraCorners(camera.zoom, camera.viewportWidth,
					camera.viewportHeight);
	}

	public float viewportWidth;
	public float zoom;

	void updateCameraCorners(float zoom, float viewportWidth,
			float viewportHeight) {
		this.zoom = zoom;
		this.viewportWidth = viewportWidth;
		final float halfWidth = viewportWidth * 0.5f * zoom;
		final float halfHeight = viewportHeight * 0.5f * zoom;
		final float x = camera.position.x;
		final float y = camera.position.y;
		x1 = x - halfWidth;
		x2 = x + halfWidth;
		y1 = y - halfHeight;
		y2 = y + halfHeight;

	}

	float x1;
	float x2;
	float y1;
	float y2;
	private GL10 gl10;
	private LightMap lightMap;

	static final int defaultMaximum = 1023;

	/**
	 * Don't call this inside of any begin/end statements. Call this method
	 * after you have rendered background but before UI. Box2d bodies can be
	 * rendered before or after depending how you want x-ray light interact with
	 * bodies
	 */
	public final void updateAndRender() {
		updateRays();
		renderLights();

	}

	// Rays
	public final void updateRays() {
		updateCameraCorners();

		final int size = lightList.size;
		for (int j = 0; j < size; j++) {
			lightList.items[j].update();
		}

	}

	public void renderLights() {
		Gdx.gl.glDepthMask(false);

		if (isGL20) {
			renderWithShaders();
		} else {
			camera.apply(gl10);
			gl10.glEnable(GL10.GL_BLEND);

			if (shadows) {
				alphaChannelClear();
			}

			gl10.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);

			final Light[] list = lightList.items;
			for (int i = 0, size = lightList.size; i < size; i++) {
				list[i].render();
			}

			if (shadows) {
				gl10.glBlendFunc(GL10.GL_ONE, GL10.GL_DST_ALPHA);
				box.render(GL10.GL_TRIANGLE_FAN, 0, 4);
				gl10.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_COLOR);
			}

			gl10.glDisable(GL10.GL_BLEND);
		}
	}

	void renderWithShaders() {
		lightMap.setLightMapPos(x1, x2, y1, y2);

		lightShader.begin();
		{
			lightShader.setUniformMatrix("u_projTrans",
					camera.combined);

			lightMap.frameBuffer.begin();

			gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

			gl20.glEnable(GL20.GL_BLEND);
			gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

			final Light[] list = lightList.items;
			for (int i = 0, size = lightList.size; i < size; i++) {
				list[i].render();
			}
			lightMap.frameBuffer.end();
		}
		lightShader.end();

		lightMap.render(camera);
	}

	private void alphaChannelClear() {
		gl10.glClearColor(0f, 0f, 0f, ambientLight);
		gl10.glColorMask(false, false, false, true);
		gl10.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl10.glColorMask(true, true, true, true);
		gl10.glClearColor(0f, 0f, 0f, 0f);

	}

	public void dispose() {
		final int size = lightList.size;
		for (int i = 0; i < size; i++) {
			lightList.items[i].lightMesh.dispose();
			lightList.items[i].softShadowMesh.dispose();
		}
		if (lightMap != null)
			lightMap.dispose();
		if (lightShader != null)
			lightShader.dispose();
	}

	float m_segments[];
	float[] m_x;
	float[] m_y;
	float[] m_f;
	int m_index = 0;

	final RayCastCallback ray = new RayCastCallback() {
		@Override
		public float reportRayFixture(Fixture fixture, Vector2 point,
				Vector2 normal, float fraction) {

			if ((filterA != null) && !contactFilter(fixture))
				return -1;

			m_x[m_index] = point.x;
			m_y[m_index] = point.y;
			m_f[m_index] = fraction;
			return fraction;
		}
	};

	/** light filter **/
	private Filter filterA = null;

	public void setContactFilter(Filter filter) {
		filterA = filter;
	}

	public void setContactFilter(short categoryBits, short groupIndex,
			short maskBits) {
		filterA = new Filter();
		filterA.categoryBits = categoryBits;
		filterA.groupIndex = groupIndex;
		filterA.maskBits = maskBits;
	}

	boolean contactFilter(Fixture fixtureB) {
		Filter filterB = fixtureB.getFilterData();

		if (filterA.groupIndex == filterB.groupIndex
					&& filterA.groupIndex != 0)
			return filterA.groupIndex > 0;

		return (filterA.maskBits & filterB.categoryBits) != 0
					&& (filterA.categoryBits & filterB.maskBits) != 0;

	}

	public void removeAll() {

		while (lightList.size > 0)
			lightList.pop().remove();
	}

	private void setShadowBox() {
		int i = 0;
		// This need some work, maybe camera matrix would needed
		float c = Color.toFloatBits(0, 0, 0, 1);

		m_segments[i++] = -100000f;
		m_segments[i++] = -100000f;
		m_segments[i++] = c;
		m_segments[i++] = -100000f;
		m_segments[i++] = 100000f;
		m_segments[i++] = c;
		m_segments[i++] = 100000f;
		m_segments[i++] = 100000f;
		m_segments[i++] = c;
		m_segments[i++] = 100000f;
		m_segments[i++] = -100000;
		m_segments[i++] = c;
		box.setVertices(m_segments, 0, i);
	}

	/**
	 * Disables/enables culling. This save cpu and gpu time when world is bigger
	 * than screen default = true
	 * 
	 * @param culling
	 *            the culling to set
	 */
	public final void setCulling(boolean culling) {
		this.culling = culling;
	}

	/**
	 * Disables/enables gaussian blur. This make lights much more softer and
	 * realistic look but also cost some precious shader time. With default fbo
	 * size on android cost around 1ms
	 * 
	 * default = true;
	 * 
	 * @param blur
	 *            the blur to set
	 */
	public final void setBlur(boolean blur) {
		this.blur = blur;
	}

	/**
	 * Set number of gaussian blur passes. Blurring can be pretty heavy weight
	 * operation, 1-3 should be safe. Setting this to 0 is same as
	 * setBlur(false)
	 * 
	 * default = 1
	 * 
	 * @param blurNum
	 *            the blurNum to set
	 */
	public final void setBlurNum(int blurNum) {
		this.blurNum = blurNum;
	}

	/**
	 * Disables/enables shadows. NOTE: If gl1.1 android you need to change
	 * render target to contain alpha channel* default = true
	 * 
	 * @param shadows
	 *            the shadows to set
	 */
	public final void setShadows(boolean shadows) {
		this.shadows = shadows;
	}

	/**
	 * Ambient light is how dark are the shadows. clamped to 0-1
	 * 
	 * default = 0;
	 * 
	 * @param ambientLight
	 *            the ambientLight to set
	 */
	public final void setAmbientLight(float ambientLight) {
		this.ambientLight = MathUtils.clamp(ambientLight, 0, 1);
	}

	/**
	 * @param world
	 *            the world to set
	 */
	public final void setWorld(World world) {
		this.world = world;
	}

}
