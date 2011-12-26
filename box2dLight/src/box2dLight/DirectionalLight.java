package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class DirectionalLight extends Light {

	float sin;
	float cos;
	final Vector2 start[];
	final Vector2 end[];
	float colorF;

	public DirectionalLight(RayHandler rayHandler, int rays, boolean isStatic,
				boolean isXray, Color color, float directionDegree,
			float distance) {

		super(rayHandler, rays, isStatic, isXray, color, directionDegree,
					distance);

		colorF = super.color.toFloatBits();
		vertexNum = (vertexNum - 1) * 2;

		start = new Vector2[rayNum];
		end = new Vector2[rayNum];
		for (int i = 0; i < rayNum; i++) {
			start[i] = new Vector2();
			end[i] = new Vector2();
		}
		setDirection(direction);

		lightMesh = new Mesh(staticLight, vertexNum, 0,
				new VertexAttribute(Usage.Position, 2, "vertex_positions"),
				new VertexAttribute(Usage.ColorPacked, 4, "quad_colors"));
		softShadowMesh = new Mesh(staticLight, vertexNum, 0,
				new VertexAttribute(Usage.Position, 2, "vertex_positions"),
				new VertexAttribute(Usage.ColorPacked, 4, "quad_colors"));

		rayHandler.lightList.add(this);
	}

	public void setDirection(float direction) {
		super.direction = direction;
		sin = MathUtils.sinDeg(direction);
		cos = MathUtils.cosDeg(direction);
	}

	@Override
	public void update() {
		if (!active && staticLight)
			return;

		final float centerX = (rayHandler.x1 + rayHandler.x2) * 0.5f;
		final float centerY = (rayHandler.y1 + rayHandler.y2) * 0.5f;
		final float size = rayHandler.viewportWidth * rayHandler.zoom * 0.5f
				* 1.41421356f;

		// final float newX = tmpX * cos - tmpY * sin;
		// final float newY = tmpX * sin + tmpY * cos;

		final float widthOff = size * -sin;
		final float heightOff = size * cos;

		final float d1 = distance * cos;
		final float d2 = distance * sin;
		final float f = 2f / rayNum;

		for (int i = 0; i < rayNum; i++) {
			final float portion = i * f;
			final float dx = portion * widthOff;
			final float dy = portion * heightOff;

			start[i].x = centerX - d1 - widthOff + dx;
			start[i].y = centerY - d2 - heightOff + dy;

			end[i].x = centerX + d1 - widthOff + dx;
			end[i].y = centerY + d2 - heightOff + dy;
		}

		for (int i = 0; i < rayNum; i++) {
			rayHandler.m_index = i;
			rayHandler.m_x[i] = end[i].x;
			rayHandler.m_y[i] = end[i].y;
			if (!xray) {
				rayHandler.world.rayCast(rayHandler.ray, start[i], end[i]);
			}
		}

		updateLightMesh();
	}

	@Override
	void updateLightMesh() {
		// ray starting point
		int size = 0;
		// rays ending points.
		final int arraySize = rayNum;
		final float seg[] = rayHandler.m_segments;
		final float m_x[] = rayHandler.m_x;
		final float m_y[] = rayHandler.m_y;

		for (int i = 0; i < arraySize; i++) {
			seg[size++] = start[i].x;
			seg[size++] = start[i].y;
			seg[size++] = colorF;

			seg[size++] = m_x[i];
			seg[size++] = m_y[i];
			seg[size++] = colorF;
		}

		lightMesh.setVertices(seg, 0, size);

		if (!soft || xray)
			return;

		size = 0;
		// rays ending points.
		final float localZero = zero;
		for (int i = 0; i < arraySize; i++) {
			seg[size++] = m_x[i];
			seg[size++] = m_y[i];
			seg[size++] = colorF;

			seg[size++] = m_x[i]
					+ softShadowLenght * cos;
			seg[size++] = m_y[i]
					+ softShadowLenght * sin;
			seg[size++] = localZero;
		}
		softShadowMesh.setVertices(seg, 0, size);

	}

	@Override
	public void render() {
		if (active) {

			if (rayHandler.isGL20) {
				lightMesh.render(rayHandler.lightShader, GL10.GL_TRIANGLE_FAN, 0,
						vertexNum);
				if (soft && !xray) {
					softShadowMesh.render(rayHandler.lightShader,
							GL10.GL_TRIANGLE_STRIP, 0, vertexNum);
				}
			} else {
				lightMesh.render(GL10.GL_TRIANGLE_STRIP, 0, vertexNum);
				if (soft && !xray) {
					softShadowMesh.render(GL10.GL_TRIANGLE_STRIP, 0,
							vertexNum);
				}
			}
		}
	}

}
