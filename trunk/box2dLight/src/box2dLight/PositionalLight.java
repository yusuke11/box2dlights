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
import com.badlogic.gdx.utils.NumberUtils;

public abstract class PositionalLight extends Light {

	private Body body;
	private float bodyOffsetX;
	private float bodyOffsetY;

	/**
	 * attach positional light to automatically follow body. Position is fixed
	 * to given offset.
	 */
	@Override
	public void attachToBody(Body body, float offsetX, float offSetY) {
		this.body = body;
		bodyOffsetX = offsetX;
		bodyOffsetY = offSetY;
	}

	@Override
	public Body getBody() {
		return body;
	}

	final float sin[];
	final float cos[];

	final Vector2 start = new Vector2();
	final float endX[];
	final float endY[];

	PositionalLight(RayHandler rayHandler, int rays, Color color,
			float distance,
			float x, float y, float directionDegree) {
		super(rayHandler, rays, color, directionDegree,
				distance);
		setPos(x, y);
		sin = new float[rays];
		cos = new float[rays];
		endX = new float[rays];
		endY = new float[rays];

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
			tmpEnd.x = endX[i] + start.x;
			rayHandler.m_x[i] = tmpEnd.x;
			tmpEnd.y = endY[i] + start.y;
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

			if (rayHandler.isGL20) {
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
		if (staticLight)
			staticUpdate();
	}

	@Override
	void updateLightMesh() {

		if (rayHandler.isGL20) {
			// ray starting point
			int size = 0;
			final float seg[] = rayHandler.m_segments;
			final float m_x[] = rayHandler.m_x;
			final float m_y[] = rayHandler.m_y;
			final float m_f[] = rayHandler.m_f;

			seg[size++] = start.x;
			seg[size++] = start.y;
			seg[size++] = colorF;
			seg[size++] = 1;
			// rays ending points.
			for (int i = 0; i < rayNum; i++) {
				seg[size++] = m_x[i];
				seg[size++] = m_y[i];
				seg[size++] = colorF;
				seg[size++] = 1 - m_f[i];
			}
			lightMesh.setVertices(seg, 0, size);

			if (!soft || xray)
				return;

			size = 0;
			// rays ending points.

			for (int i = 0; i < rayNum; i++) {
				seg[size++] = m_x[i];
				seg[size++] = m_y[i];
				seg[size++] = colorF;
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
		} else {
			final float r = color.r * 255;
			final float g = color.g * 255;
			final float b = color.b * 255;
			final float a = color.a * 255;
			// ray starting point
			final float seg[] = rayHandler.m_segments;
			final float m_x[] = rayHandler.m_x;
			final float m_y[] = rayHandler.m_y;
			final float m_f[] = rayHandler.m_f;
			int size = 0;
			seg[size++] = start.x;
			seg[size++] = start.y;
			seg[size++] = colorF;
			seg[size++] = 0f;
			// rays ending points.
			for (int i = 0; i < rayNum; i++) {
				seg[size++] = m_x[i];
				seg[size++] = m_y[i];
				final float s = 1f - m_f[i];
				seg[size++] = Float.intBitsToFloat(((int) (a * s) << 24)
						| ((int) (b * s) << 16) | ((int) (g * s) << 8)
						| ((int) (r * s)) & 0xfeffffff);
				seg[size++] = 0f;
			}
			lightMesh.setVertices(seg, 0, size);

			if (!soft || xray)
				return;

			size = 0;
			for (int i = 0; i < rayNum; i++) {
				seg[size++] = m_x[i];
				seg[size++] = m_y[i];
				final float s = 1f - m_f[i];
				seg[size++] = Float.intBitsToFloat(((int) (a * s) << 24)
						| ((int) (b * s) << 16) | ((int) (g * s) << 8)
						| ((int) (r * s)) & 0xfeffffff);
				seg[size++] = 0f;

				seg[size++] = m_x[i]
						+ softShadowLenght * cos[i];

				seg[size++] = m_y[i]
						+ softShadowLenght * sin[i];
				seg[size++] = zero;
				seg[size++] = 0f;
			}
			softShadowMesh.setVertices(seg, 0, size);
		}

	}
}
