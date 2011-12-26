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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class RayHandler {

	public static int FBO_W = 400;
	public static int FBO_H = 240;

	public static boolean shadows = true;
	public static boolean blur = true;
	public static final int blurNum = 4;
	public static float ambientLight = 0.0f;

	final static int MIN_RAYS = 3;
	int MAX_RAYS;
	private GL20 gl20;
	public Mesh box;
	public World world;
	boolean culling = false;
	public OrthographicCamera camera;
	public ShaderProgram lightShader;

	static boolean isGL20;

	final public Array<Light> lightList = new Array<Light>(
			false, 16,
			Light.class);

	/**
	 * cam need to be set for this feature
	 */
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

	public float x1;
	public float x2;
	public float y1;
	public float y2;
	private GL10 gl10;
	private LightMap lightMap;

	static final int defaultMaximum = 1023;

	public RayHandler(World world, OrthographicCamera camera) {
		this(world, camera, defaultMaximum);
	}

	public RayHandler(World world) {
		this(world, null, defaultMaximum);
	}

	public RayHandler(World world, OrthographicCamera camera, int maxRayCount) {
		if (maxRayCount < MIN_RAYS) {
			maxRayCount = MIN_RAYS;
		}
		this.world = world;
		MAX_RAYS = maxRayCount;
		this.camera = camera;
		updateCameraCorners();
		m_segments = new float[maxRayCount * 8];
		m_x = new float[maxRayCount];
		m_y = new float[maxRayCount];
		m_f = new float[maxRayCount];

		isGL20 = Gdx.graphics.isGL20Available();
		if (isGL20) {

			lightMap = new LightMap(x1, x2, y1, y2);
			lightShader = LightShader.createLightShader();
		}

		if (isGL20)
			gl20 = Gdx.graphics.getGL20();
		else {
			gl10 = Gdx.graphics.getGL10();
			box = new Mesh(true, 12, 0, new VertexAttribute(Usage.Position, 2,
			"vertex_positions"), new VertexAttribute(Usage.ColorPacked,
			4,"quad_colors"));
			setShadowBox();

		}

	}

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

		for (int i = lightList.size - 1; i >= 0; i--) {
			lightList.items[i].remove();
		}
		lightList.clear();
	}

	private void setShadowBox() {
		int i = 0;
		// This need some work, maybe camera matrix would needed
		float c = Color.toFloatBits(0, 0, 0, 1);

		m_segments[i++] = -1000f;
		m_segments[i++] = -1000f;
		m_segments[i++] = c;
		m_segments[i++] = -1000f;
		m_segments[i++] = 1000f;
		m_segments[i++] = c;
		m_segments[i++] = 1000f;
		m_segments[i++] = 1000f;
		m_segments[i++] = c;
		m_segments[i++] = 1000f;
		m_segments[i++] = -1000;
		m_segments[i++] = c;
		box.setVertices(m_segments, 0, i);

	}

}
