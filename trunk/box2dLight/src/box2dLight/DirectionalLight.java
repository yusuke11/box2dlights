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

public class DirectionalLight extends Light {

	float sin;
	float cos;
	final Vector2 start[];
	final Vector2 end[];

	/**
	 * Directional lights simulate light source that locations is at infinite
	 * distance. Direction and intensity is same everywhere. -90 direction is
	 * straight from up.
	 * 
	 * @param rayHandler
	 * @param rays
	 * @param color
	 * @param directionDegree
	 * @param distance
	 */
	public DirectionalLight(RayHandler rayHandler, int rays, Color color,
			float directionDegree) {

		super(rayHandler, rays, color, directionDegree, Float.POSITIVE_INFINITY);

		vertexNum = (vertexNum - 1) * 2;

		start = new Vector2[rayNum];
		end = new Vector2[rayNum];
		for (int i = 0; i < rayNum; i++) {
			start[i] = new Vector2();
			end[i] = new Vector2();
		}
		setDirection(direction);

		lightMesh = new Mesh(staticLight, vertexNum, 0, new VertexAttribute(
				Usage.Position, 2, "vertex_positions"), new VertexAttribute(
				Usage.ColorPacked, 4, "quad_colors"), new VertexAttribute(
				Usage.Generic, 1, "s"));
		softShadowMesh = new Mesh(staticLight, vertexNum, 0,
				new VertexAttribute(Usage.Position, 2, "vertex_positions"),
				new VertexAttribute(Usage.ColorPacked, 4, "quad_colors"),
				new VertexAttribute(Usage.Generic, 1, "s"));
		update();
	}

	@Override
	public void setDirection(float direction) {
		super.direction = direction;
		sin = MathUtils.sinDeg(direction);
		cos = MathUtils.cosDeg(direction);
		if (staticLight)
			staticUpdate();
	}

	@Override
	void update() {
		if (!active && staticLight)
			return;

		// sqrt2 = 1.41421356f;
		final float sizeOfScreen = (rayHandler.x2 - rayHandler.x1) * 0.5f * 1.41421356f;

		final float widthOff = sizeOfScreen * -sin;
		final float heightOff = sizeOfScreen * cos;

		final float x = (rayHandler.x1 + rayHandler.x2) * 0.5f - widthOff;
		final float y = (rayHandler.y1 + rayHandler.y2) * 0.5f - heightOff;

		float xAxelOffSet = sizeOfScreen * cos;
		float yAxelOffSet = sizeOfScreen * sin;

		// checking against rayCast length <= 0 assertion error
		
		if ((xAxelOffSet * xAxelOffSet + yAxelOffSet * yAxelOffSet) < 0.1f) {
			xAxelOffSet = 0.1f;
			yAxelOffSet = 0.1f;
		}

		final float portion = 2f / rayNum;
		for (int i = 0; i < rayNum; i++) {
			final float steppedX = i * portion * widthOff + x;
			final float steppedY = i * portion * heightOff + y;

			rayHandler.m_index = i;
			start[i].x = steppedX - xAxelOffSet;
			start[i].y = steppedY - yAxelOffSet;
			
			
			rayHandler.m_x[i] = end[i].x = steppedX + xAxelOffSet;
			rayHandler.m_y[i] = end[i].y = steppedY + yAxelOffSet;

			if (rayHandler.world != null && !xray) {
				rayHandler.world.rayCast(rayHandler.ray, start[i], end[i]);
			}
		}
		
		
		// update light mesh
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
			seg[size++] = 1f;
			seg[size++] = m_x[i];
			seg[size++] = m_y[i];
			seg[size++] = colorF;
			seg[size++] = 1f;
		}

		lightMesh.setVertices(seg, 0, size);

		if (!soft || xray)
			return;

		size = 0;
		// rays ending points.
		for (int i = 0; i < arraySize; i++) {
			seg[size++] = m_x[i];
			seg[size++] = m_y[i];
			seg[size++] = colorF;
			seg[size++] = 1f;

			seg[size++] = m_x[i] + softShadowLenght * cos;
			seg[size++] = m_y[i] + softShadowLenght * sin;
			seg[size++] = zero;
			seg[size++] = 1f;
		}
		softShadowMesh.setVertices(seg, 0, size);

	}

	@Override
	void render() {
		if (active) {

			if (rayHandler.isGL20) {
				lightMesh.render(rayHandler.lightShader,
						GL20.GL_TRIANGLE_STRIP, 0, vertexNum);
				rayHandler.lightRenderedLastFrame++;
				if (soft && !xray) {
					softShadowMesh.render(rayHandler.lightShader,
							GL20.GL_TRIANGLE_STRIP, 0, vertexNum);
				}
			} else {
				lightMesh.render(GL10.GL_TRIANGLE_STRIP, 0, vertexNum);
				rayHandler.lightRenderedLastFrame++;
				if (soft && !xray) {
					softShadowMesh.render(GL10.GL_TRIANGLE_STRIP, 0, vertexNum);
				}
			}
		}
	}

	@Override
	final public void attachToBody(Body body, float offsetX, float offSetY) {
	}

	@Override
	public void setPosition(float x, float y) {
	}

	@Override
	public Body getBody() {
		return null;
	}

	@Override
	public float getX() {
		return 0;
	}

	@Override
	public float getY() {
		return 0;
	}

	@Override
	public void setPosition(Vector2 position) {
	}

}
