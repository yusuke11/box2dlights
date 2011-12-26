package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class PointLight extends PositionalLight {

	public PointLight(RayHandler rayHandler, int rays, boolean isStatic,
			boolean isXray, Color color, float distance,
			float x, float y) {
		super(rayHandler, rays, isStatic, isXray, color, distance, x, y, 0f);
		setEndPoints();
		setPos(start.x, start.y);
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
			setPos(vec.x + dX, vec.y + dY);
		}

		if (rayHandler.culling)
			if (testCull())
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

	public void setPos(float x, float y) {
		start.x = x;
		start.y = y;

		if (staticLight) {
			staticLight = false;
			update();
			staticLight = true;
		}
	}

	private final void setEndPoints() {
		for (int i = 0; i < rayNum; i++) {
			float angle = direction + 360
					* i / (rayNum - 1);
			final float s = sin[i] = MathUtils.sinDeg(angle);
			final float c = cos[i] = MathUtils.cosDeg(angle);
			end[i].set(distance * c, distance * s);
		}
	}

	@Override
	public void setDirection(float directionDegree) {
		// TODO Auto-generated method stub

	}

}
