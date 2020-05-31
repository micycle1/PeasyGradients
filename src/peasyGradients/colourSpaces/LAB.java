package peasyGradients.colourSpaces;

import net.jafama.FastMath;

/**
 * https://github.com/gka/chroma.js/blob/master/src/io/lab/rgb2lab.js CIE-L*ab
 * 
 * @author micycle1
 *
 */
public final class LAB {

	// D65 standard referent
	private static final double Xn = 0.950470;
	private static final double Yn = 1.0;
	private static final double Zn = 1.088830;

	private static final double t0 = 0.137931034; // 4 / 29
	private static final double t2 = 0.12841855; // 3 * t1 * t1
	private static final double t3 = 0.008856452; // t1 * t1 * t1

	private static final double third = 1 / 3.0f;

	/**
	 * sRGB (0-255) to LAB (CIE-L*ab)
	 * 
	 * @param rgb
	 * @return
	 */
	public static double[] rgb2lab(float[] rgb) {
		double[] xyz = rgb2xyz(rgb[0], rgb[1], rgb[2]);
		double l = 116 * xyz[1] - 16;
		if (l < 0) {
			return new double[0];
		}
		return new double[] { l, 500 * (xyz[0] - xyz[1]), 200 * (xyz[1] - xyz[2]) };
	}

	private static double rgb_xyz(double r) {
		if ((r /= 255) <= 0.04045)
			return r / 12.92;
		return FastMath.pow((r + 0.055) / 1.055, 2.4);
	}

	private static double xyz_lab(double t) {
		if (t > t3) {
			return FastMath.pow(t, third);
		} else {
			return t / t2 + t0;
		}
	}

	private static double[] rgb2xyz(double r, double g, double b) {
		r = rgb_xyz(r);
		g = rgb_xyz(g);
		b = rgb_xyz(b);
		final double x = xyz_lab((0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / Xn);
		final double y = xyz_lab((0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / Yn);
		final double z = xyz_lab((0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / Zn);
		return new double[] { x, y, z };
	}

	// --------------------------------------------------------------------------------------------

	static final double Xn2 = 95.04;
	static final double Yn2 = 100;
	static final double Zn2 = 108.89;

	public static int lab2rgb(double[] lab) {
		return xyz2rgb(lab2xyz(lab));
	}

	/**
	 * https://github.com/Heanzy/bupt-wechatapp/tree/master/src/main/java/com/buptcc/wechatapp/utils
	 * 
	 * @param Lab
	 * @return
	 */
	private static double[] lab2xyz(double[] Lab) {
		double[] XYZ = new double[3];
		final double L, a, b;
		final double fx, fy, fz;

		L = Lab[0];
		a = Lab[1];
		b = Lab[2];

		fy = (L + 16) / 116;
		fx = a / 500 + fy;
		fz = fy - b / 200;

		if (fx > 0.2069) {
			XYZ[0] = (Xn2 * fx * fx * fx);
		} else {
			XYZ[0] = Xn2 * (fx - 0.1379f) * 0.1284f;
		}

		if ((fy > 0.2069) || (L > 8)) {
			XYZ[1] = (Yn2 * fy * fy * fy);
		} else {
			XYZ[1] = Yn2 * (fy - 0.1379f) * 0.1284f;
		}

		if (fz > 0.2069) {
			XYZ[2] = (Zn2 * fz * fz * fz);
		} else {
			XYZ[2] = Zn2 * (fz - 0.1379f) * 0.1284f;
		}

		return XYZ;
	}

	private static int xyz2rgb(double[] XYZ) {
		final double X, Y, Z;
		double dr, dg, db;
		X = XYZ[0];
		Y = XYZ[1];
		Z = XYZ[2];

		dr = 0.032406 * X - 0.015371 * Y - 0.0049895 * Z;
		dg = -0.0096891 * X + 0.018757 * Y + 0.00041914 * Z;
		db = 0.00055708 * X - 0.0020401 * Y + 0.01057 * Z;

		if (dr <= 0.00313) {
			dr = dr * 12.92;
		} else {
			dr = (FastMath.exp(FastMath.log(dr) / 2.4) * 1.055 - 0.055);
		}

		if (dg <= 0.00313) {
			dg = dg * 12.92;
		} else {
			dg = (FastMath.exp(FastMath.log(dg) / 2.4) * 1.055 - 0.055);
		}

		if (db <= 0.00313) {
			db = db * 12.92;
		} else {
			db = (FastMath.exp(FastMath.log(db) / 2.4) * 1.055 - 0.055);
		}

//		dr = (dr > 1) ? 1 : dr;
		dr *= 255;

//		dg = (dg > 1) ? 1 : dg;
		dg *= 255;

//		db = (db > 1) ? 1 : db;
		db *= 255;

		return 255 << 24 | (int) (dr + 0.5) << 16 | (int) (dg + 0.5) << 8 | (int) (db + 0.5);
	}

	/**
	 * originclr, destclr, scaledst, rsltclr
	 * 
	 * @param a   LAB col 1
	 * @param b   LAB col 2
	 * @param st  step
	 * @param out new col
	 * @return
	 */
	public static double[] interpolate(double[] a, double[] b, float step, double[] out) {
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}
}
