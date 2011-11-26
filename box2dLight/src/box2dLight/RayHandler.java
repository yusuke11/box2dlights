package box2dLight;

/**
 * @author kalle_h
 *
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class RayHandler {

	final int MIN_RAYS = 3;
	int MAX_RAYS;
	private GL10 gl;
	public Mesh box;
	public World world;
	boolean culling = false;
	public OrthographicCamera camera;
	/**
	 * This option need frame buffer with alpha channel You also need create box
	 * mesh by hand
	 */
	public static boolean shadows = false;
	public float ambientLight = 0.2f;
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
		if (camera != null)
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
		gl = Gdx.graphics.getGL10();
		m_segments = new float[maxRayCount * 6];
		m_x = new float[maxRayCount];
		m_y = new float[maxRayCount];
		m_f = new float[maxRayCount];
		box = new Mesh(true, 12, 0, new VertexAttribute(Usage.Position, 2,
				"vertex_positions"), new VertexAttribute(Usage.ColorPacked, 4,
				"quad_colors"));
		setShadowBox();
	}

	/**
	 * Don't call this inside of any begin/end statements. Call this method
	 * after you have rendered background but before UI. Box2d bodies can be
	 * rendered before or after depending how you want x-ray light interact with
	 * bodies
	 */
	public final void updateAndRender() {
		updateRays();
		if (shadows) {
			renderLightsAndShadows();
		} else {
			renderLights();
		}
	}

	// Rays
	public final void updateRays() {
		updateCameraCorners();

		final int size = lightList.size;
		for (int j = 0; j < size; j++) {
			lightList.items[j].update();
		}

	}

	/**
	 * REMEMBER CALL updateRays(World world) BEFORE render. Don't call this
	 * inside of any begin/end statements. Call this method after you have
	 * rendered background but before UI Box2d bodies can be rendered before or
	 * after depending how you want x-ray light interact with bodies
	 */
	public void renderLightsAndShadows() {
		if (shadows) {
			alphaChannelClear();
		}
		renderLights();
		if (shadows) {
			renderShadows();
		}
	}

	public void renderLights() {
		if (camera != null) {
			camera.apply(gl);
		}
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);

		final Light[] list = lightList.items;
		for (int i = 0, size = lightList.size; i < size; i++) {
			list[i].render();
		}

		gl.glDisable(GL10.GL_BLEND);
	}

	/**
	 * call alphaChannelClear() before light rendering and after lights call
	 * this. Use renderLightAndShadows() for simplicity
	 */
	private void renderShadows() {

		gl.glEnable(GL10.GL_BLEND);
		// rendering shadow box over screen
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_DST_ALPHA);
		box.render(GL10.GL_TRIANGLE_FAN, 0, 4);

		gl.glDisable(GL10.GL_BLEND);

	}

	private void alphaChannelClear() {
		// clearing the alpha channel
		gl.glClearColor(0f, 0f, 0f, ambientLight);
		gl.glColorMask(false, false, false, true);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl.glColorMask(true, true, true, true);
		gl.glClearColor(0f, 0f, 0f, 1f);

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
			m_x[m_index] = point.x;
			m_y[m_index] = point.y;
			m_f[m_index] = fraction;
			return fraction;
		}
	};

	private void setShadowBox() {
		int i = 0;
		// This need some work, maybe camera matrix would needed
		float c = Color
				.toFloatBits(0, 0, 0, 1);

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

	public void removeAll() {

		for (int i = lightList.size - 1; i >= 0; i--) {
			lightList.items[i].remove();
		}
		lightList.clear();
	}

}
