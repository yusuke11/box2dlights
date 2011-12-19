package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Light is data container for all the light parameters You can create instance
 * of Light also with help of rayHandler addLight method
 */
public class ConeLight extends PositionalLight {

	float coneDegree;

	/**
	 * @param rayHandler
	 * @param rays
	 * @param isStatic
	 * @param isXray
	 * @param directionDegree
	 * @param distance
	 * @param color
	 * @param x
	 * @param y
	 * @param coneDegree
	 */
	public ConeLight(RayHandler rayHandler, int rays, boolean isStatic,
			boolean isXray, Color color, float distance,
			float x, float y, float directionDegree, float coneDegree) {

		super(rayHandler, rays, isStatic, isXray, color, distance, x, y,
				directionDegree);
		this.coneDegree = coneDegree;
		setPosAndDirection(start.x, start.y, direction);
	}

	public void setDirection(float direction) {
		setPosAndDirection(start.x, start.y, direction);
	}

	public void setPos(float x, float y) {
		setPosAndDirection(x, y, direction);
	}

	/**
	 * set the starting point and direction, call this if you need to change
	 * both positon and rotation for slightly better perfirmance
	 */
	public void setPosAndDirection(float x, float y, float direction) {

		start.x = x;
		start.y = y;
		this.direction = direction;
		for (int i = 0; i < rayNum; i++) {
			float angle = direction + coneDegree - 2f * coneDegree
					* i / (rayNum - 1f);
			final float s = sin[i] = MathUtils.sinDeg(angle);
			final float c = cos[i] = MathUtils.cosDeg(angle);
			end[i].set(x + distance * c, y + distance * s);
		}
		if (staticLight) {
			staticLight = false;
			update();
			staticLight = true;
		}
	}

	@Override
	public void update() {
		if (!active || staticLight)
			return;

		float angle = 0;
		if (body != null) {
			Vector2 vec = body.getPosition();
			angle += body.getAngle() * MathUtils.radiansToDegrees;
			final float cos = MathUtils.cosDeg(angle);
			final float sin = MathUtils.sinDeg(angle);
			final float dX = bodyOffsetX * cos - bodyOffsetY * sin;
			final float dY = bodyOffsetX * sin + bodyOffsetY * cos;
			start.set(vec.x + dX, vec.y + dY);
		}

		if (rayHandler.culling)
			if (testCull())
				return;

		if (body != null) {
			this.setPosAndDirection(start.x, start.y,
					direction + angle);
			direction -=angle;
			
		}

		for (int i = 0; i < rayNum; i++) {
			rayHandler.m_index = i;
			rayHandler.m_f[i] = 1f;
			rayHandler.m_x[i] = end[i].x;
			rayHandler.m_y[i] = end[i].y;
			if (!xray) {
				rayHandler.world.rayCast(rayHandler.ray, start, end[i]);
			}
		}

		updateLightMesh();
	}
}
