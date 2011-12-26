package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public abstract class PositionalLight extends Light {

	public Body body;
	public float bodyOffsetX;
	public float bodyOffsetY;

	/**
	 * attach positional light to automatically follow body. Position is fixed
	 * to given offset
	 */
	public void attachToBody(Body body, float offsetX, float offSetY) {
		this.body = body;
		bodyOffsetX = offsetX;
		bodyOffsetY = offSetY;
	}

	final float sin[];
	final float cos[];

	final Vector2 start = new Vector2();
	final Vector2 end[];

	PositionalLight(RayHandler rayHandler, int rays, boolean isStatic,
			boolean isXray, Color color, float distance,
			float x, float y, float directionDegree) {

		super(rayHandler, rays, isStatic, isXray, color, directionDegree,
				distance);
		setPos(x, y);
		sin = new float[rays];
		cos = new float[rays];
		end = new Vector2[rays];
		for (int i = 0; i < rays; i++) {
			end[i] = new Vector2();
		}

		lightMesh = new Mesh(staticLight, vertexNum, 0,
				new VertexAttribute(Usage.Position, 2, "vertex_positions"),
				new VertexAttribute(Usage.ColorPacked, 4, "quad_colors"),
				new VertexAttribute(Usage.Generic, 1, "s"));
		softShadowMesh = new Mesh(staticLight, vertexNum * 2, 0,
				new VertexAttribute(Usage.Position, 2, "vertex_positions"),
				new VertexAttribute(Usage.ColorPacked, 4, "quad_colors")
				, new VertexAttribute(Usage.Generic, 1, "s"));
		rayHandler.lightList.add(this);
	}

	private final Vector2 tmpEnd = new Vector2();

	@Override
	public void update() {
		if (!active || staticLight)
			return;

		if (body != null) {
			final Vector2 vec = body.getPosition();
			float angle = body.getAngle();
			final float cos = MathUtils.cos(angle);
			final float sin = MathUtils.sin(angle);
			final float dX = bodyOffsetX * cos - bodyOffsetY * sin;
			final float dY = bodyOffsetX * sin + bodyOffsetY * cos;
			start.x = vec.x + dX;
			start.y = vec.y + dY;
		}

		if (rayHandler.culling)
			if ((culled = !rayHandler.intersect(start.x, start.y, distance)))
				return;

		for (int i = 0; i < rayNum; i++) {
			rayHandler.m_index = i;
			rayHandler.m_f[i] = 1f;
			tmpEnd.x = end[i].x + start.x;
			rayHandler.m_x[i] = tmpEnd.x;
			tmpEnd.y = end[i].y + start.y;
			rayHandler.m_y[i] = tmpEnd.y;
			if (!xray) {
				rayHandler.world.rayCast(rayHandler.ray, start, tmpEnd);
			}
		}
		updateLightMesh();
	}

	@Override
	public void render() {
		if (active && !culled) {

			if (RayHandler.isGL20) {
				lightMesh.render(rayHandler.lightShader, GL20.GL_TRIANGLE_FAN,
						0,
						vertexNum);
				if (soft && !xray) {
					softShadowMesh.render(rayHandler.lightShader,
							GL20.GL_TRIANGLE_STRIP, 0, (vertexNum - 1) * 2);
				}
			} else {
				lightMesh.render(GL10.GL_TRIANGLE_FAN, 0, vertexNum);
				if (soft && !xray) {
					softShadowMesh.render(GL10.GL_TRIANGLE_STRIP, 0,
							(vertexNum - 1) * 2);
				}
			}
		}
	}

	@Override
	public void setPos(float x, float y) {
		start.x = x;
		start.y = y;

		if (staticLight) {
			staticLight = false;
			update();
			staticLight = true;
		}
	}

	@Override
	void updateLightMesh() {

		if (!RayHandler.isGL20) {
			final float r = color.r;
			final float g = color.g;
			final float b = color.b;
			final float a = color.a;
			// ray starting point
			int size = 0;
			final float seg[] = rayHandler.m_segments;
			final float m_x[] = rayHandler.m_x;
			final float m_y[] = rayHandler.m_y;
			final float m_f[] = rayHandler.m_f;

			seg[size++] = start.x;
			seg[size++] = start.y;
			seg[size++] = Color.toFloatBits(r, g, b, a);
			seg[size++] = 0f;
			// rays ending points.
			final int arraySize = rayNum;
			for (int i = 0; i < arraySize; i++) {
				seg[size++] = m_x[i];
				seg[size++] = m_y[i];
				final float s = 1f - m_f[i];
				seg[size++] = Color.toFloatBits(r * s,
						g * s, b * s, a * s);
				seg[size++] = 0f;
			}
			lightMesh.setVertices(seg, 0, size);

			if (!soft || xray)
				return;

			size = 0;

			for (int i = 0; i < arraySize; i++) {
				seg[size++] = m_x[i];
				seg[size++] = m_y[i];
				final float s = 1f - m_f[i];
				seg[size++] = Color.toFloatBits(r * s,
						g * s, b * s, a * s);
				seg[size++] = 0f;
				seg[size++] = m_x[i]
						+ softShadowLenght * cos[i];

				seg[size++] = m_y[i]
						+ softShadowLenght * sin[i];
				seg[size++] = zero;
				seg[size++] = 0f;
			}
			softShadowMesh.setVertices(seg, 0, size);

		} else {
			final float colorBits = color.toFloatBits();
			// ray starting point
			int size = 0;
			final float seg[] = rayHandler.m_segments;
			final float m_x[] = rayHandler.m_x;
			final float m_y[] = rayHandler.m_y;
			final float m_f[] = rayHandler.m_f;

			seg[size++] = start.x;
			seg[size++] = start.y;
			seg[size++] = colorBits;
			seg[size++] = 1;
			// rays ending points.
			final int arraySize = rayNum;
			for (int i = 0; i < arraySize; i++) {
				seg[size++] = m_x[i];
				seg[size++] = m_y[i];
				seg[size++] = colorBits;
				seg[size++] = 1 - m_f[i];
			}
			lightMesh.setVertices(seg, 0, size);

			if (!soft || xray)
				return;

			size = 0;
			// rays ending points.

			for (int i = 0; i < arraySize; i++) {
				seg[size++] = m_x[i];
				seg[size++] = m_y[i];
				seg[size++] = colorBits;
				final float s = (1 - m_f[i]);
				seg[size++] = s;
				seg[size++] = m_x[i]
						+ s * softShadowLenght * cos[i];
				seg[size++] = m_y[i]
						+ s * softShadowLenght * sin[i];
				seg[size++] = zero;
				seg[size++] = 0f;
			}
			softShadowMesh.setVertices(seg, 0, size);
		}
	}
}
