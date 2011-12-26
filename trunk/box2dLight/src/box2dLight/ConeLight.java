package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

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
		setDirection(direction);
	}

	public void setDirection(float direction) {

		this.direction = direction;
		for (int i = 0; i < rayNum; i++) {
			float angle = direction + coneDegree - 2f * coneDegree
					* i / (rayNum - 1f);
			final float s = sin[i] = MathUtils.sinDeg(angle);
			final float c = cos[i] = MathUtils.cosDeg(angle);
			end[i].set(distance * c, distance * s);
		}
		if (staticLight) {
			staticLight = false;
			update();
			staticLight = true;
		}
	}

}
