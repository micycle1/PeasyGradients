package peasyGradients;

import processing.core.PApplet;

/**
 * https://github.com/gka/chroma.js/blob/master/src/io/lab/rgb2lab.js CIE-L*ab
 * 
 * @author micycle1
 *
 */
final class LAB {

	// D65 standard referent
	private static final float Xn = 0.950470f;
	private static final float Yn = 1;
	private static final float Zn = 1.088830f;

	private static final float t0 = 0.137931034f; // 4 / 29
	private static final float t1 = 0.206896552f; // 6 / 29
	private static final float t2 = 0.12841855f; // 3 * t1 * t1
	private static final float t3 = 0.008856452f; // t1 * t1 * t1

	/**
	 * sRGB (0-255) to LAB (CIE-L*ab)
	 * 
	 * @param rgb
	 * @return
	 */
	static float[] rgb2lab(float[] rgb) {
		float[] xyz = rgb2xyz(rgb[0], rgb[1], rgb[2]);
		float l = 116 * xyz[1] - 16;
//		if (l < 0) {
//			return new float[0]; // todo?
//		}
		return new float[] { l, 500 * (xyz[0] - xyz[1]), 200 * (xyz[1] - xyz[2]) };
	}

	private static float rgb_xyz(float r) {
		if ((r /= 255) <= 0.04045f)
			return r / 12.92f;
		return PApplet.pow((r + 0.055f) / 1.055f, 2.4f);
	}

	private static float xyz_lab(float t) {
		if (t > t3) {
			return PApplet.pow(t, (1 / 3.0f));
		} else {
			return t / t2 + t0;
		}
	}

	 static float[] rgb2xyz(float r, float g, float b) {
		r = rgb_xyz(r);
		g = rgb_xyz(g);
		b = rgb_xyz(b);
		final float x = xyz_lab((0.4124564f * r + 0.3575761f * g + 0.1804375f * b) / Xn);
		final float y = xyz_lab((0.2126729f * r + 0.7151522f * g + 0.0721750f * b) / Yn);
		final float z = xyz_lab((0.0193339f * r + 0.1191920f * g + 0.9503041f * b) / Zn);
		return new float[] { x, y, z };
	}

	// --------------------------------------------------------------------------------------------

	/**
	 * L* [0..100] a [-100..100] b [-100..100]
	 * 
	 * @param lab
	 * @return
	 * @deprecated
	 */
	static float[] lab2rgb(float[] lab) {

		float y = (lab[0] + 16) / 116;
//	    float x = isNaN(a) ? y : y + lab[1] / 500;
//	    float z = isNaN(b) ? y : y - lab[2] / 200;

		float x = y + lab[1] / 500;
		float z = lab[2] / 200;

		y = Yn * lab_xyz(y);
		x = Xn * lab_xyz(x);
		z = Zn * lab_xyz(z);

		float r = xyz_rgb(3.2404542f * x - 1.5371385f * y - 0.4985314f * z); // D65 -> sRGB
		float g = xyz_rgb(-0.9692660f * x + 1.8760108f * y + 0.0415560f * z);
		float b_ = xyz_rgb(0.0556434f * x - 0.2040259f * y + 1.0572252f * z);

//	    return [r,g,b_,args.length > 3 ? args[3] : 1];
		return new float[] { r, g, b_ };
	};

	/**
	 * XYZ->RGB
	 * 
	 * @param r
	 * @return
	 * @deprecated
	 */
	private static float xyz_rgb(float r) {
//		System.out.println(255 * (r <= 0.00304f ? 12.92f * r : 1.055f * PApplet.pow(r, 1 / 2.4f) - 0.055f));
		return 255 * (r <= 0.00304f ? 12.92f * r : 1.055f * PApplet.pow(r, 1 / 2.4f) - 0.055f);
	}

	/**
	 * @deprecated
	 * @param t
	 * @return
	 */
	private static float lab_xyz(float t) {
//		System.out.println(t);
//		System.out.println(t > t1 ? t * t * t : t2 * (t - t0));
		return t > t1 ? t * t * t : t2 * (t - t0);
	}

	/**
	 * https://github.com/Heanzy/bupt-wechatapp/tree/master/src/main/java/com/buptcc/wechatapp/utils
	 * 
	 * @param Lab
	 * @return
	 */
	private static float[] Lab2XYZ(float[] Lab) {
		float[] XYZ = new float[3];
		float L, a, b;
		float fx, fy, fz;
		float Xn, Yn, Zn;
		Xn = 95.04f;
		Yn = 100;
		Zn = 108.89f;

		L = Lab[0];
		a = Lab[1];
		b = Lab[2];

		fy = (L + 16) / 116;
		fx = a / 500 + fy;
		fz = fy - b / 200;

		if (fx > 0.2069) {
			XYZ[0] = (float) (Xn * Math.pow(fx, 3));
		} else {
			XYZ[0] = Xn * (fx - 0.1379f) * 0.1284f;
		}

		if ((fy > 0.2069) || (L > 8)) {
			XYZ[1] = (float) (Yn * Math.pow(fy, 3));
		} else {
			XYZ[1] = Yn * (fy - 0.1379f) * 0.1284f;
		}

		if (fz > 0.2069) {
			XYZ[2] = (float) (Zn * Math.pow(fz, 3));
		} else {
			XYZ[2] = Zn * (fz - 0.1379f) * 0.1284f;
		}

		return XYZ;
	}

	static float[] XYZ2sRGB(float[] XYZ) {
		float[] sRGB = new float[3];
		float X, Y, Z;
		float dr, dg, db;
		X = XYZ[0];
		Y = XYZ[1];
		Z = XYZ[2];

		dr = 0.032406f * X - 0.015371f * Y - 0.0049895f * Z;
		dg = -0.0096891f * X + 0.018757f * Y + 0.00041914f * Z;
		db = 0.00055708f * X - 0.0020401f * Y + 0.01057f * Z;

		if (dr <= 0.00313) {
			dr = dr * 12.92f;
		} else {
			dr = (float) (Math.exp(Math.log(dr) / 2.4) * 1.055 - 0.055);
		}

		if (dg <= 0.00313) {
			dg = dg * 12.92f;
		} else {
			dg = (float) (Math.exp(Math.log(dg) / 2.4) * 1.055 - 0.055);
		}

		if (db <= 0.00313) {
			db = db * 12.92f;
		} else {
			db = (float) (Math.exp(Math.log(db) / 2.4) * 1.055 - 0.055);
		}

		dr = dr * 255; // 255 here TODO
		dg = dg * 255;
		db = db * 255;

		dr = Math.min(255, dr);
		dg = Math.min(255, dg);
		db = Math.min(255, db);

		sRGB[0] = (int) (dr + 0.5);
		sRGB[1] = (int) (dg + 0.5);
		sRGB[2] = (int) (db + 0.5);

		return sRGB;
	}

	public static float[] lab2rgb2(float[] lab) {
		return XYZ2sRGB(Lab2XYZ(lab));
	}
	
	/**
	 * originclr, destclr, scaledst, rsltclr
	 * @param a LAB col 1
	 * @param b LAB col 2
	 * @param st step
	 * @param out new col
	 * @return
	 */
	static float[] interpolate(float[] a, float[] b, float step, float[] out) {
//		out = new float[3];
		step = Functions.calcStep(step);
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}

	// interpolate
//	const lab = (col1, col2, f) => {
//	    const xyz0 = col1.lab();
//	    const xyz1 = col2.lab();
//	    return new Color(
//	        xyz0[0] + f * (xyz1[0]-xyz0[0]),
//	        xyz0[1] + f * (xyz1[1]-xyz0[1]),
//	        xyz0[2] + f * (xyz1[2]-xyz0[2]),
//	        'lab'
//	    )
//	}
}
