package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class PointLight extends PositionalLight {

	public PointLight(RayHandler rayHandler, int rays, boolean isStatic,
			boolean isXray, Color color, float distance,
			float x, float y) {
		super(rayHandler, rays, isStatic, isXray, color, distance, x, y, 0f);
		setEndPoints();		
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
