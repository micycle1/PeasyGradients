package peasyGradients.utilities;

//MIT License

//
//Copyright(c) 2020 Jordan Peck (jordan.me2@gmail.com)
//Copyright(c) 2020 Contributors
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files(the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions :
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

/**
 * A reduced version of <a href= "https://github.com/Auburn/FastNoise">FastNoise Lite</a> for PeasyGradients.
 */
public class FastNoiseLite {

	public enum NoiseType {
		OpenSimplex2, OpenSimplex2S, Cellular, Perlin, ValueCubic, Value
	};

	public enum RotationType3D {
		None, ImproveXYPlanes, ImproveXZPlanes
	};

	public enum FractalType {
		None, FBm, Ridged, PingPong, DomainWarpProgressive, DomainWarpIndependent
	};

	public enum CellularDistanceFunction {
		Euclidean, EuclideanSq, Manhattan, Hybrid
	};

	public enum CellularReturnType {
		CellValue, Distance, Distance2, Distance2Add, Distance2Sub, Distance2Mul, Distance2Div
	};

	public enum DomainWarpType {
		OpenSimplex2, OpenSimplex2Reduced, BasicGrid
	};

	private int mSeed = 1337;
	private float mFrequency = 0.01f;
	private NoiseType mNoiseType = NoiseType.OpenSimplex2;
	private RotationType3D mRotationType3D = RotationType3D.None;
	private FractalType mFractalType = FractalType.None;
	private int mOctaves = 3;
	private float mLacunarity = 2.0f;
	private float mGain = 0.5f;
	private float mWeightedStrength = 0.0f;
	private float mPingPongStength = 2.0f;

	private float mFractalBounding = 1 / 1.75f;

	private CellularDistanceFunction mCellularDistanceFunction = CellularDistanceFunction.EuclideanSq;
	private CellularReturnType mCellularReturnType = CellularReturnType.Distance;
	private float mCellularJitterModifier = 1.0f;

	private DomainWarpType mDomainWarpType = DomainWarpType.OpenSimplex2;
	private float mDomainWarpAmp = 1.0f;

	private static final float SQRT3 = 1.7320508075688772935274463415059f;
	private static final float F2 = 0.5f * (SQRT3 - 1); // magic constant
	private static final float G2 = (3 - SQRT3) / 6; // magic constant

	/**
	 * Create new FastNoise object with default seed
	 */
	public FastNoiseLite() {
	}

	/// <summary>
	/// Create new FastNoise object with specified seed
	/// </summary>
	public FastNoiseLite(int seed) {
		SetSeed(seed);
	}

	/// <summary>
	/// Sets seed used for all noise types
	/// </summary>
	/// <remarks>
	/// Default: 1337
	/// </remarks>
	public void SetSeed(int seed) {
		mSeed = seed;
	}

	/// <summary>
	/// Sets frequency for all noise types
	/// </summary>
	/// <remarks>
	/// Default: 0.01
	/// </remarks>
	public void SetFrequency(float frequency) {
		mFrequency = frequency;
	}

	/// <summary>
	/// Sets noise algorithm used for GetNoise(...)
	/// </summary>
	/// <remarks>
	/// Default: OpenSimplex2
	/// </remarks>
	public void SetNoiseType(NoiseType noiseType) {
		mNoiseType = noiseType;
		UpdateTransformType3D();
	}

	/// <summary>
	/// Sets domain rotation type for 3D Noise and 3D DomainWarp.
	/// Can aid in reducing directional artifacts when sampling a 2D plane in 3D
	/// </summary>
	/// <remarks>
	/// Default: None
	/// </remarks>
	public void SetRotationType3D(RotationType3D rotationType3D) {
		mRotationType3D = rotationType3D;
		UpdateTransformType3D();
		UpdateWarpTransformType3D();
	}

	/// <summary>
	/// Sets method for combining octaves in all fractal noise types
	/// </summary>
	/// <remarks>
	/// Default: None
	/// Note: FractalType.DomainWarp... only affects DomainWarp(...)
	/// </remarks>
	public void SetFractalType(FractalType fractalType) {
		mFractalType = fractalType;
	}

	/// <summary>
	/// Sets octave count for all fractal noise types
	/// </summary>
	/// <remarks>
	/// Default: 3
	/// </remarks>
	public void SetFractalOctaves(int octaves) {
		mOctaves = octaves;
		CalculateFractalBounding();
	}

	/// <summary>
	/// Sets octave lacunarity for all fractal noise types
	/// </summary>
	/// <remarks>
	/// Default: 2.0
	/// </remarks>
	public void SetFractalLacunarity(float lacunarity) {
		mLacunarity = lacunarity;
	}

	/// <summary>
	/// Sets octave gain for all fractal noise types
	/// </summary>
	/// <remarks>
	/// Default: 0.5
	/// </remarks>
	public void SetFractalGain(float gain) {
		mGain = gain;
		CalculateFractalBounding();
	}

	/// <summary>
	/// Sets octave weighting for all none DomainWarp fratal types
	/// </summary>
	/// <remarks>
	/// Default: 0.0
	/// Note: Keep between 0...1 to maintain -1...1 output bounding
	/// </remarks>
	public void SetFractalWeightedStrength(float weightedStrength) {
		mWeightedStrength = weightedStrength;
	}

	/// <summary>
	/// Sets strength of the fractal ping pong effect
	/// </summary>
	/// <remarks>
	/// Default: 2.0
	/// </remarks>
	public void SetFractalPingPongStrength(float pingPongStrength) {
		mPingPongStength = pingPongStrength;
	}

	/// <summary>
	/// Sets distance function used in cellular noise calculations
	/// </summary>
	/// <remarks>
	/// Default: Distance
	/// </remarks>
	public void SetCellularDistanceFunction(CellularDistanceFunction cellularDistanceFunction) {
		mCellularDistanceFunction = cellularDistanceFunction;
	}

	/// <summary>
	/// Sets return type from cellular noise calculations
	/// </summary>
	/// <remarks>
	/// Default: EuclideanSq
	/// </remarks>
	public void SetCellularReturnType(CellularReturnType cellularReturnType) {
		mCellularReturnType = cellularReturnType;
	}

	/// <summary>
	/// Sets the maximum distance a cellular point can move from it's grid position
	/// </summary>
	/// <remarks>
	/// Default: 1.0
	/// Note: Setting this higher than 1 will cause artifacts
	/// </remarks>
	public void SetCellularJitter(float cellularJitter) {
		mCellularJitterModifier = cellularJitter;
	}

	/// <summary>
	/// Sets the warp algorithm when using DomainWarp(...)
	/// </summary>
	/// <remarks>
	/// Default: OpenSimplex2
	/// </remarks>
	public void SetDomainWarpType(DomainWarpType domainWarpType) {
		mDomainWarpType = domainWarpType;
		UpdateWarpTransformType3D();
	}

	/// <summary>
	/// Sets the maximum warp distance from original position when using
	/// DomainWarp(...)
	/// </summary>
	/// <remarks>
	/// Default: 1.0
	/// </remarks>
	public void SetDomainWarpAmp(float domainWarpAmp) {
		mDomainWarpAmp = domainWarpAmp;
	}

	/// <summary>
	/// 2D noise at given position using current settings
	/// </summary>
	/// <returns>
	/// Noise output bounded between -1...1
	/// </returns>
	public float GetNoise(float x, float y) {
		x *= mFrequency;
		y *= mFrequency;

		switch (mNoiseType) {
			case OpenSimplex2 :
			case OpenSimplex2S : {
				float t = (x + y) * F2;
				x += t;
				y += t;
			}
				break;
			default :
				break;
		}

		switch (mFractalType) {
			default :
				return GenNoiseSingle(mSeed, x, y);
			case FBm :
				return GenFractalFBm(x, y);
			case Ridged :
				return GenFractalRidged(x, y);
			case PingPong :
				return GenFractalPingPong(x, y);
		}
	}

	/**
	 * Custom implementation for PeasyGradients. A little less overhead choosing the
	 * noise type etc.
	 * 
	 * @return Simplex noise between 0...1
	 * @author micycle1
	 */
	public float getSimplexNoiseFast(float x, float y) {
		x *= mFrequency;
		y *= mFrequency;
		float t = (x + y) * F2;
		x += t;
		y += t;
		return (SingleSimplex(mSeed, x, y) + 1) / 2; // get simplex noise and map from [-1...1] to [0...1]
	}

	/// <summary>
	/// 2D warps the input position using current domain warp settings
	/// </summary>
	/// <example>
	/// Example usage with GetNoise
	/// <code>DomainWarp(coord)
	/// noise = GetNoise(x, y)</code>
	/// </example>
	public void DomainWarp(Vector2 coord) {
		switch (mFractalType) {
			default :
				DomainWarpSingle(coord);
				break;
			case DomainWarpProgressive :
				DomainWarpFractalProgressive(coord);
				break;
			case DomainWarpIndependent :
				DomainWarpFractalIndependent(coord);
				break;
		}
	}

	private static final float[] Gradients2D = { 0.130526192220052f, 0.99144486137381f, 0.38268343236509f, 0.923879532511287f,
			0.608761429008721f, 0.793353340291235f, 0.793353340291235f, 0.608761429008721f, 0.923879532511287f, 0.38268343236509f,
			0.99144486137381f, 0.130526192220051f, 0.99144486137381f, -0.130526192220051f, 0.923879532511287f, -0.38268343236509f,
			0.793353340291235f, -0.60876142900872f, 0.608761429008721f, -0.793353340291235f, 0.38268343236509f, -0.923879532511287f,
			0.130526192220052f, -0.99144486137381f, -0.130526192220052f, -0.99144486137381f, -0.38268343236509f, -0.923879532511287f,
			-0.608761429008721f, -0.793353340291235f, -0.793353340291235f, -0.608761429008721f, -0.923879532511287f, -0.38268343236509f,
			-0.99144486137381f, -0.130526192220052f, -0.99144486137381f, 0.130526192220051f, -0.923879532511287f, 0.38268343236509f,
			-0.793353340291235f, 0.608761429008721f, -0.608761429008721f, 0.793353340291235f, -0.38268343236509f, 0.923879532511287f,
			-0.130526192220052f, 0.99144486137381f, 0.130526192220052f, 0.99144486137381f, 0.38268343236509f, 0.923879532511287f,
			0.608761429008721f, 0.793353340291235f, 0.793353340291235f, 0.608761429008721f, 0.923879532511287f, 0.38268343236509f,
			0.99144486137381f, 0.130526192220051f, 0.99144486137381f, -0.130526192220051f, 0.923879532511287f, -0.38268343236509f,
			0.793353340291235f, -0.60876142900872f, 0.608761429008721f, -0.793353340291235f, 0.38268343236509f, -0.923879532511287f,
			0.130526192220052f, -0.99144486137381f, -0.130526192220052f, -0.99144486137381f, -0.38268343236509f, -0.923879532511287f,
			-0.608761429008721f, -0.793353340291235f, -0.793353340291235f, -0.608761429008721f, -0.923879532511287f, -0.38268343236509f,
			-0.99144486137381f, -0.130526192220052f, -0.99144486137381f, 0.130526192220051f, -0.923879532511287f, 0.38268343236509f,
			-0.793353340291235f, 0.608761429008721f, -0.608761429008721f, 0.793353340291235f, -0.38268343236509f, 0.923879532511287f,
			-0.130526192220052f, 0.99144486137381f, 0.130526192220052f, 0.99144486137381f, 0.38268343236509f, 0.923879532511287f,
			0.608761429008721f, 0.793353340291235f, 0.793353340291235f, 0.608761429008721f, 0.923879532511287f, 0.38268343236509f,
			0.99144486137381f, 0.130526192220051f, 0.99144486137381f, -0.130526192220051f, 0.923879532511287f, -0.38268343236509f,
			0.793353340291235f, -0.60876142900872f, 0.608761429008721f, -0.793353340291235f, 0.38268343236509f, -0.923879532511287f,
			0.130526192220052f, -0.99144486137381f, -0.130526192220052f, -0.99144486137381f, -0.38268343236509f, -0.923879532511287f,
			-0.608761429008721f, -0.793353340291235f, -0.793353340291235f, -0.608761429008721f, -0.923879532511287f, -0.38268343236509f,
			-0.99144486137381f, -0.130526192220052f, -0.99144486137381f, 0.130526192220051f, -0.923879532511287f, 0.38268343236509f,
			-0.793353340291235f, 0.608761429008721f, -0.608761429008721f, 0.793353340291235f, -0.38268343236509f, 0.923879532511287f,
			-0.130526192220052f, 0.99144486137381f, 0.130526192220052f, 0.99144486137381f, 0.38268343236509f, 0.923879532511287f,
			0.608761429008721f, 0.793353340291235f, 0.793353340291235f, 0.608761429008721f, 0.923879532511287f, 0.38268343236509f,
			0.99144486137381f, 0.130526192220051f, 0.99144486137381f, -0.130526192220051f, 0.923879532511287f, -0.38268343236509f,
			0.793353340291235f, -0.60876142900872f, 0.608761429008721f, -0.793353340291235f, 0.38268343236509f, -0.923879532511287f,
			0.130526192220052f, -0.99144486137381f, -0.130526192220052f, -0.99144486137381f, -0.38268343236509f, -0.923879532511287f,
			-0.608761429008721f, -0.793353340291235f, -0.793353340291235f, -0.608761429008721f, -0.923879532511287f, -0.38268343236509f,
			-0.99144486137381f, -0.130526192220052f, -0.99144486137381f, 0.130526192220051f, -0.923879532511287f, 0.38268343236509f,
			-0.793353340291235f, 0.608761429008721f, -0.608761429008721f, 0.793353340291235f, -0.38268343236509f, 0.923879532511287f,
			-0.130526192220052f, 0.99144486137381f, 0.130526192220052f, 0.99144486137381f, 0.38268343236509f, 0.923879532511287f,
			0.608761429008721f, 0.793353340291235f, 0.793353340291235f, 0.608761429008721f, 0.923879532511287f, 0.38268343236509f,
			0.99144486137381f, 0.130526192220051f, 0.99144486137381f, -0.130526192220051f, 0.923879532511287f, -0.38268343236509f,
			0.793353340291235f, -0.60876142900872f, 0.608761429008721f, -0.793353340291235f, 0.38268343236509f, -0.923879532511287f,
			0.130526192220052f, -0.99144486137381f, -0.130526192220052f, -0.99144486137381f, -0.38268343236509f, -0.923879532511287f,
			-0.608761429008721f, -0.793353340291235f, -0.793353340291235f, -0.608761429008721f, -0.923879532511287f, -0.38268343236509f,
			-0.99144486137381f, -0.130526192220052f, -0.99144486137381f, 0.130526192220051f, -0.923879532511287f, 0.38268343236509f,
			-0.793353340291235f, 0.608761429008721f, -0.608761429008721f, 0.793353340291235f, -0.38268343236509f, 0.923879532511287f,
			-0.130526192220052f, 0.99144486137381f, 0.38268343236509f, 0.923879532511287f, 0.923879532511287f, 0.38268343236509f,
			0.923879532511287f, -0.38268343236509f, 0.38268343236509f, -0.923879532511287f, -0.38268343236509f, -0.923879532511287f,
			-0.923879532511287f, -0.38268343236509f, -0.923879532511287f, 0.38268343236509f, -0.38268343236509f, 0.923879532511287f, };

	private static final float[] RandVecs2D = { -0.2700222198f, -0.9628540911f, 0.3863092627f, -0.9223693152f, 0.04444859006f,
			-0.999011673f, -0.5992523158f, -0.8005602176f, -0.7819280288f, 0.6233687174f, 0.9464672271f, 0.3227999196f, -0.6514146797f,
			-0.7587218957f, 0.9378472289f, 0.347048376f, -0.8497875957f, -0.5271252623f, -0.879042592f, 0.4767432447f, -0.892300288f,
			-0.4514423508f, -0.379844434f, -0.9250503802f, -0.9951650832f, 0.0982163789f, 0.7724397808f, -0.6350880136f, 0.7573283322f,
			-0.6530343002f, -0.9928004525f, -0.119780055f, -0.0532665713f, 0.9985803285f, 0.9754253726f, -0.2203300762f, -0.7665018163f,
			0.6422421394f, 0.991636706f, 0.1290606184f, -0.994696838f, 0.1028503788f, -0.5379205513f, -0.84299554f, 0.5022815471f,
			-0.8647041387f, 0.4559821461f, -0.8899889226f, -0.8659131224f, -0.5001944266f, 0.0879458407f, -0.9961252577f, -0.5051684983f,
			0.8630207346f, 0.7753185226f, -0.6315704146f, -0.6921944612f, 0.7217110418f, -0.5191659449f, -0.8546734591f, 0.8978622882f,
			-0.4402764035f, -0.1706774107f, 0.9853269617f, -0.9353430106f, -0.3537420705f, -0.9992404798f, 0.03896746794f, -0.2882064021f,
			-0.9575683108f, -0.9663811329f, 0.2571137995f, -0.8759714238f, -0.4823630009f, -0.8303123018f, -0.5572983775f, 0.05110133755f,
			-0.9986934731f, -0.8558373281f, -0.5172450752f, 0.09887025282f, 0.9951003332f, 0.9189016087f, 0.3944867976f, -0.2439375892f,
			-0.9697909324f, -0.8121409387f, -0.5834613061f, -0.9910431363f, 0.1335421355f, 0.8492423985f, -0.5280031709f, -0.9717838994f,
			-0.2358729591f, 0.9949457207f, 0.1004142068f, 0.6241065508f, -0.7813392434f, 0.662910307f, 0.7486988212f, -0.7197418176f,
			0.6942418282f, -0.8143370775f, -0.5803922158f, 0.104521054f, -0.9945226741f, -0.1065926113f, -0.9943027784f, 0.445799684f,
			-0.8951327509f, 0.105547406f, 0.9944142724f, -0.992790267f, 0.1198644477f, -0.8334366408f, 0.552615025f, 0.9115561563f,
			-0.4111755999f, 0.8285544909f, -0.5599084351f, 0.7217097654f, -0.6921957921f, 0.4940492677f, -0.8694339084f, -0.3652321272f,
			-0.9309164803f, -0.9696606758f, 0.2444548501f, 0.08925509731f, -0.996008799f, 0.5354071276f, -0.8445941083f, -0.1053576186f,
			0.9944343981f, -0.9890284586f, 0.1477251101f, 0.004856104961f, 0.9999882091f, 0.9885598478f, 0.1508291331f, 0.9286129562f,
			-0.3710498316f, -0.5832393863f, -0.8123003252f, 0.3015207509f, 0.9534596146f, -0.9575110528f, 0.2883965738f, 0.9715802154f,
			-0.2367105511f, 0.229981792f, 0.9731949318f, 0.955763816f, -0.2941352207f, 0.740956116f, 0.6715534485f, -0.9971513787f,
			-0.07542630764f, 0.6905710663f, -0.7232645452f, -0.290713703f, -0.9568100872f, 0.5912777791f, -0.8064679708f, -0.9454592212f,
			-0.325740481f, 0.6664455681f, 0.74555369f, 0.6236134912f, 0.7817328275f, 0.9126993851f, -0.4086316587f, -0.8191762011f,
			0.5735419353f, -0.8812745759f, -0.4726046147f, 0.9953313627f, 0.09651672651f, 0.9855650846f, -0.1692969699f, -0.8495980887f,
			0.5274306472f, 0.6174853946f, -0.7865823463f, 0.8508156371f, 0.52546432f, 0.9985032451f, -0.05469249926f, 0.1971371563f,
			-0.9803759185f, 0.6607855748f, -0.7505747292f, -0.03097494063f, 0.9995201614f, -0.6731660801f, 0.739491331f, -0.7195018362f,
			-0.6944905383f, 0.9727511689f, 0.2318515979f, 0.9997059088f, -0.0242506907f, 0.4421787429f, -0.8969269532f, 0.9981350961f,
			-0.061043673f, -0.9173660799f, -0.3980445648f, -0.8150056635f, -0.5794529907f, -0.8789331304f, 0.4769450202f, 0.0158605829f,
			0.999874213f, -0.8095464474f, 0.5870558317f, -0.9165898907f, -0.3998286786f, -0.8023542565f, 0.5968480938f, -0.5176737917f,
			0.8555780767f, -0.8154407307f, -0.5788405779f, 0.4022010347f, -0.9155513791f, -0.9052556868f, -0.4248672045f, 0.7317445619f,
			0.6815789728f, -0.5647632201f, -0.8252529947f, -0.8403276335f, -0.5420788397f, -0.9314281527f, 0.363925262f, 0.5238198472f,
			0.8518290719f, 0.7432803869f, -0.6689800195f, -0.985371561f, -0.1704197369f, 0.4601468731f, 0.88784281f, 0.825855404f,
			0.5638819483f, 0.6182366099f, 0.7859920446f, 0.8331502863f, -0.553046653f, 0.1500307506f, 0.9886813308f, -0.662330369f,
			-0.7492119075f, -0.668598664f, 0.743623444f, 0.7025606278f, 0.7116238924f, -0.5419389763f, -0.8404178401f, -0.3388616456f,
			0.9408362159f, 0.8331530315f, 0.5530425174f, -0.2989720662f, -0.9542618632f, 0.2638522993f, 0.9645630949f, 0.124108739f,
			-0.9922686234f, -0.7282649308f, -0.6852956957f, 0.6962500149f, 0.7177993569f, -0.9183535368f, 0.3957610156f, -0.6326102274f,
			-0.7744703352f, -0.9331891859f, -0.359385508f, -0.1153779357f, -0.9933216659f, 0.9514974788f, -0.3076565421f, -0.08987977445f,
			-0.9959526224f, 0.6678496916f, 0.7442961705f, 0.7952400393f, -0.6062947138f, -0.6462007402f, -0.7631674805f, -0.2733598753f,
			0.9619118351f, 0.9669590226f, -0.254931851f, -0.9792894595f, 0.2024651934f, -0.5369502995f, -0.8436138784f, -0.270036471f,
			-0.9628500944f, -0.6400277131f, 0.7683518247f, -0.7854537493f, -0.6189203566f, 0.06005905383f, -0.9981948257f, -0.02455770378f,
			0.9996984141f, -0.65983623f, 0.751409442f, -0.6253894466f, -0.7803127835f, -0.6210408851f, -0.7837781695f, 0.8348888491f,
			0.5504185768f, -0.1592275245f, 0.9872419133f, 0.8367622488f, 0.5475663786f, -0.8675753916f, -0.4973056806f, -0.2022662628f,
			-0.9793305667f, 0.9399189937f, 0.3413975472f, 0.9877404807f, -0.1561049093f, -0.9034455656f, 0.4287028224f, 0.1269804218f,
			-0.9919052235f, -0.3819600854f, 0.924178821f, 0.9754625894f, 0.2201652486f, -0.3204015856f, -0.9472818081f, -0.9874760884f,
			0.1577687387f, 0.02535348474f, -0.9996785487f, 0.4835130794f, -0.8753371362f, -0.2850799925f, -0.9585037287f, -0.06805516006f,
			-0.99768156f, -0.7885244045f, -0.6150034663f, 0.3185392127f, -0.9479096845f, 0.8880043089f, 0.4598351306f, 0.6476921488f,
			-0.7619021462f, 0.9820241299f, 0.1887554194f, 0.9357275128f, -0.3527237187f, -0.8894895414f, 0.4569555293f, 0.7922791302f,
			0.6101588153f, 0.7483818261f, 0.6632681526f, -0.7288929755f, -0.6846276581f, 0.8729032783f, -0.4878932944f, 0.8288345784f,
			0.5594937369f, 0.08074567077f, 0.9967347374f, 0.9799148216f, -0.1994165048f, -0.580730673f, -0.8140957471f, -0.4700049791f,
			-0.8826637636f, 0.2409492979f, 0.9705377045f, 0.9437816757f, -0.3305694308f, -0.8927998638f, -0.4504535528f, -0.8069622304f,
			0.5906030467f, 0.06258973166f, 0.9980393407f, -0.9312597469f, 0.3643559849f, 0.5777449785f, 0.8162173362f, -0.3360095855f,
			-0.941858566f, 0.697932075f, -0.7161639607f, -0.002008157227f, -0.9999979837f, -0.1827294312f, -0.9831632392f, -0.6523911722f,
			0.7578824173f, -0.4302626911f, -0.9027037258f, -0.9985126289f, -0.05452091251f, -0.01028102172f, -0.9999471489f, -0.4946071129f,
			0.8691166802f, -0.2999350194f, 0.9539596344f, 0.8165471961f, 0.5772786819f, 0.2697460475f, 0.962931498f, -0.7306287391f,
			-0.6827749597f, -0.7590952064f, -0.6509796216f, -0.907053853f, 0.4210146171f, -0.5104861064f, -0.8598860013f, 0.8613350597f,
			0.5080373165f, 0.5007881595f, -0.8655698812f, -0.654158152f, 0.7563577938f, -0.8382755311f, -0.545246856f, 0.6940070834f,
			0.7199681717f, 0.06950936031f, 0.9975812994f, 0.1702942185f, -0.9853932612f, 0.2695973274f, 0.9629731466f, 0.5519612192f,
			-0.8338697815f, 0.225657487f, -0.9742067022f, 0.4215262855f, -0.9068161835f, 0.4881873305f, -0.8727388672f, -0.3683854996f,
			-0.9296731273f, -0.9825390578f, 0.1860564427f, 0.81256471f, 0.5828709909f, 0.3196460933f, -0.9475370046f, 0.9570913859f,
			0.2897862643f, -0.6876655497f, -0.7260276109f, -0.9988770922f, -0.047376731f, -0.1250179027f, 0.992154486f, -0.8280133617f,
			0.560708367f, 0.9324863769f, -0.3612051451f, 0.6394653183f, 0.7688199442f, -0.01623847064f, -0.9998681473f, -0.9955014666f,
			-0.09474613458f, -0.81453315f, 0.580117012f, 0.4037327978f, -0.9148769469f, 0.9944263371f, 0.1054336766f, -0.1624711654f,
			0.9867132919f, -0.9949487814f, -0.100383875f, -0.6995302564f, 0.7146029809f, 0.5263414922f, -0.85027327f, -0.5395221479f,
			0.841971408f, 0.6579370318f, 0.7530729462f, 0.01426758847f, -0.9998982128f, -0.6734383991f, 0.7392433447f, 0.639412098f,
			-0.7688642071f, 0.9211571421f, 0.3891908523f, -0.146637214f, -0.9891903394f, -0.782318098f, 0.6228791163f, -0.5039610839f,
			-0.8637263605f, -0.7743120191f, -0.6328039957f, };

	private static float FastMin(float a, float b) {
		return a < b ? a : b;
	}

	private static float FastMax(float a, float b) {
		return a > b ? a : b;
	}

	private static float FastAbs(float f) {
		return f < 0 ? -f : f;
	}

	private static float FastSqrt(float f) {
		return (float) Math.sqrt(f);
	}

	private static int FastFloor(float f) {
		return f >= 0 ? (int) f : (int) f - 1;
	}

	private static int FastRound(float f) {
		return f >= 0 ? (int) (f + 0.5f) : (int) (f - 0.5f);
	}

	private static float Lerp(float a, float b, float t) {
		return a + t * (b - a);
	}

	private static float InterpHermite(float t) {
		return t * t * (3 - 2 * t);
	}

	private static float InterpQuintic(float t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}

	private static float CubicLerp(float a, float b, float c, float d, float t) {
		float p = (d - c) - (a - b);
		return t * t * t * p + t * t * ((a - b) - p) + t * (c - a) + b;
	}

	private static float PingPong(float t) {
		t -= (int) (t * 0.5f) * 2;
		return t < 1 ? t : 2 - t;
	}

	private void CalculateFractalBounding() {
		float gain = FastAbs(mGain);
		float amp = gain;
		float ampFractal = 1.0f;
		for (int i = 1; i < mOctaves; i++) {
			ampFractal += amp;
			amp *= gain;
		}
		mFractalBounding = 1 / ampFractal;
	}

	// Hashing
	private static final int PrimeX = 501125321;
	private static final int PrimeY = 1136930381;

	private static int Hash(int seed, int xPrimed, int yPrimed) {
		int hash = seed ^ xPrimed ^ yPrimed;

		hash *= 0x27d4eb2d;
		return hash;
	}

	private static final float m1 = 1 / 2147483648.0f;

	private static float ValCoord(int seed, int xPrimed, int yPrimed) {
		int hash = Hash(seed, xPrimed, yPrimed);

		hash *= hash;
		hash ^= hash << 19;
		return hash * m1;
	}

	private static float GradCoord(int seed, int xPrimed, int yPrimed, float xd, float yd) {
		int hash = Hash(seed, xPrimed, yPrimed);
		hash ^= hash >> 15;
		hash &= 127 << 1;

		float xg = Gradients2D[hash];
		float yg = Gradients2D[hash | 1];

		return xd * xg + yd * yg;
	}

	// Generic noise gen

	private float GenNoiseSingle(int seed, float x, float y) {
		switch (mNoiseType) {
			case OpenSimplex2 :
				return SingleSimplex(seed, x, y);
			case OpenSimplex2S :
				return SingleOpenSimplex2S(seed, x, y);
			case Cellular :
				return SingleCellular(seed, x, y);
			case Perlin :
				return SinglePerlin(seed, x, y);
			case ValueCubic :
				return SingleValueCubic(seed, x, y);
			case Value :
				return SingleValue(seed, x, y);
			default :
				return 0;
		}
	}

	// Noise Coordinate Transforms (frequency, and possible skew or rotation)

	private void UpdateTransformType3D() {
		switch (mRotationType3D) {
			case ImproveXYPlanes :
				break;
			case ImproveXZPlanes :
				break;
			default :
				switch (mNoiseType) {
					case OpenSimplex2 :
					case OpenSimplex2S :
						break;
					default :
						break;
				}
				break;
		}
	}

	private void UpdateWarpTransformType3D() {
		switch (mRotationType3D) {
			case ImproveXYPlanes :
				break;
			case ImproveXZPlanes :
				break;
			default :
				switch (mDomainWarpType) {
					case OpenSimplex2 :
					case OpenSimplex2Reduced :
						break;
					default :
						break;
				}
				break;
		}
	}

	// Fractal FBm

	private float GenFractalFBm(float x, float y) {
		int seed = mSeed;
		float sum = 0;
		float amp = mFractalBounding;

		for (int i = 0; i < mOctaves; i++) {
			float noise = GenNoiseSingle(seed++, x, y);
			sum += noise * amp;
			amp *= Lerp(1.0f, FastMin(noise + 1, 2) * 0.5f, mWeightedStrength);

			x *= mLacunarity;
			y *= mLacunarity;
			amp *= mGain;
		}

		return sum;
	}

	// Fractal Ridged

	private float GenFractalRidged(float x, float y) {
		int seed = mSeed;
		float sum = 0;
		float amp = mFractalBounding;

		for (int i = 0; i < mOctaves; i++) {
			float noise = FastAbs(GenNoiseSingle(seed++, x, y));
			sum += (noise * -2 + 1) * amp;
			amp *= Lerp(1.0f, 1 - noise, mWeightedStrength);

			x *= mLacunarity;
			y *= mLacunarity;
			amp *= mGain;
		}

		return sum;
	}

	// Fractal PingPong

	private float GenFractalPingPong(float x, float y) {
		int seed = mSeed;
		float sum = 0;
		float amp = mFractalBounding;

		for (int i = 0; i < mOctaves; i++) {
			float noise = PingPong((GenNoiseSingle(seed++, x, y) + 1) * mPingPongStength);
			sum += (noise - 0.5f) * 2 * amp;
			amp *= Lerp(1.0f, noise, mWeightedStrength);

			x *= mLacunarity;
			y *= mLacunarity;
			amp *= mGain;
		}

		return sum;
	}

	// Simplex/OpenSimplex2 Noise

	private float SingleSimplex(int seed, float x, float y) {
		// 2D OpenSimplex2 case uses the same algorithm as ordinary Simplex.

		/*
		 * --- Skew moved to switch statements before fractal evaluation --- final
		 * FNLfloat F2 = 0.5f * (SQRT3 - 1); FNLfloat s = (x + y) * F2; x += s; y += s;
		 */

//		int i = FastFloor(x); // NOTE peasygradients
		int i = (int) x;
//		int j = FastFloor(y); // NOTE peasygradients
		int j = (int) y;
		float xi = (float) (x - i);
		float yi = (float) (y - j);

		float t = (xi + yi) * G2;
		float x0 = (float) (xi - t);
		float y0 = (float) (yi - t);

		i *= PrimeX;
		j *= PrimeY;

		float n0, n1, n2;

		float a = 0.5f - x0 * x0 - y0 * y0;
		if (a <= 0)
			n0 = 0;
		else {
			n0 = (a * a) * (a * a) * GradCoord(seed, i, j, x0, y0);
		}

		float c = (float) (2 * (1 - 2 * G2) * (1 / G2 - 2)) * t + ((float) (-2 * (1 - 2 * G2) * (1 - 2 * G2)) + a);
		if (c <= 0)
			n2 = 0;
		else {
			float x2 = x0 + (2 * (float) G2 - 1);
			float y2 = y0 + (2 * (float) G2 - 1);
			n2 = (c * c) * (c * c) * GradCoord(seed, i + PrimeX, j + PrimeY, x2, y2);
		}

		if (y0 > x0) {
			float x1 = x0 + (float) G2;
			float y1 = y0 + ((float) G2 - 1);
			float b = 0.5f - x1 * x1 - y1 * y1;
			if (b <= 0)
				n1 = 0;
			else {
				n1 = (b * b) * (b * b) * GradCoord(seed, i, j + PrimeY, x1, y1);
			}
		} else {
			float x1 = x0 + ((float) G2 - 1);
			float y1 = y0 + (float) G2;
			float b = 0.5f - x1 * x1 - y1 * y1;
			if (b <= 0)
				n1 = 0;
			else {
				n1 = (b * b) * (b * b) * GradCoord(seed, i + PrimeX, j, x1, y1);
			}
		}

		return (n0 + n1 + n2) * 99.83685446303647f;
	}

	// OpenSimplex2S Noise

	private float SingleOpenSimplex2S(int seed, float x, float y) {
		// 2D OpenSimplex2S case is a modified 2D simplex noise.

		/*
		 * --- Skew moved to TransformNoiseCoordinate method --- final FNLfloat F2 =
		 * 0.5f * (SQRT3 - 1); FNLfloat s = (x + y) * F2; x += s; y += s;
		 */

		int i = FastFloor(x);
		int j = FastFloor(y);
		float xi = (float) (x - i);
		float yi = (float) (y - j);

		i *= PrimeX;
		j *= PrimeY;
		int i1 = i + PrimeX;
		int j1 = j + PrimeY;

		float t = (xi + yi) * G2;
		float x0 = xi - t;
		float y0 = yi - t;

		float a0 = (2.0f / 3.0f) - x0 * x0 - y0 * y0;
		float value = (a0 * a0) * (a0 * a0) * GradCoord(seed, i, j, x0, y0);

		float a1 = (float) (2 * (1 - 2 * G2) * (1 / G2 - 2)) * t + ((float) (-2 * (1 - 2 * G2) * (1 - 2 * G2)) + a0);
		float x1 = x0 - (float) (1 - 2 * G2);
		float y1 = y0 - (float) (1 - 2 * G2);
		value += (a1 * a1) * (a1 * a1) * GradCoord(seed, i1, j1, x1, y1);

		// Nested conditionals were faster than compact bit logic/arithmetic.
		float xmyi = xi - yi;
		if (t > G2) {
			if (xi + xmyi > 1) {
				float x2 = x0 + (float) (3 * G2 - 2);
				float y2 = y0 + (float) (3 * G2 - 1);
				float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
				if (a2 > 0) {
					value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i + (PrimeX << 1), j + PrimeY, x2, y2);
				}
			} else {
				float x2 = x0 + (float) G2;
				float y2 = y0 + (float) (G2 - 1);
				float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
				if (a2 > 0) {
					value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i, j + PrimeY, x2, y2);
				}
			}

			if (yi - xmyi > 1) {
				float x3 = x0 + (float) (3 * G2 - 1);
				float y3 = y0 + (float) (3 * G2 - 2);
				float a3 = (2.0f / 3.0f) - x3 * x3 - y3 * y3;
				if (a3 > 0) {
					value += (a3 * a3) * (a3 * a3) * GradCoord(seed, i + PrimeX, j + (PrimeY << 1), x3, y3);
				}
			} else {
				float x3 = x0 + (float) (G2 - 1);
				float y3 = y0 + (float) G2;
				float a3 = (2.0f / 3.0f) - x3 * x3 - y3 * y3;
				if (a3 > 0) {
					value += (a3 * a3) * (a3 * a3) * GradCoord(seed, i + PrimeX, j, x3, y3);
				}
			}
		} else {
			if (xi + xmyi < 0) {
				float x2 = x0 + (float) (1 - G2);
				float y2 = y0 - (float) G2;
				float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
				if (a2 > 0) {
					value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i - PrimeX, j, x2, y2);
				}
			} else {
				float x2 = x0 + (float) (G2 - 1);
				float y2 = y0 + (float) G2;
				float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
				if (a2 > 0) {
					value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i + PrimeX, j, x2, y2);
				}
			}

			if (yi < xmyi) {
				float x2 = x0 - (float) G2;
				float y2 = y0 - (float) (G2 - 1);
				float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
				if (a2 > 0) {
					value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i, j - PrimeY, x2, y2);
				}
			} else {
				float x2 = x0 + (float) G2;
				float y2 = y0 + (float) (G2 - 1);
				float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
				if (a2 > 0) {
					value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i, j + PrimeY, x2, y2);
				}
			}
		}

		return value * 18.24196194486065f;
	}

	// Cellular Noise

	private float SingleCellular(int seed, float x, float y) {
		int xr = FastRound(x);
		int yr = FastRound(y);

		float distance0 = Float.MAX_VALUE;
		float distance1 = Float.MAX_VALUE;
		int closestHash = 0;

		float cellularJitter = 0.43701595f * mCellularJitterModifier;

		int xPrimed = (xr - 1) * PrimeX;
		int yPrimedBase = (yr - 1) * PrimeY;

		switch (mCellularDistanceFunction) {
			default :
			case Euclidean :
			case EuclideanSq :
				for (int xi = xr - 1; xi <= xr + 1; xi++) {
					int yPrimed = yPrimedBase;

					for (int yi = yr - 1; yi <= yr + 1; yi++) {
						int hash = Hash(seed, xPrimed, yPrimed);
						int idx = hash & (255 << 1);

						float vecX = (float) (xi - x) + RandVecs2D[idx] * cellularJitter;
						float vecY = (float) (yi - y) + RandVecs2D[idx | 1] * cellularJitter;

						float newDistance = vecX * vecX + vecY * vecY;

						distance1 = FastMax(FastMin(distance1, newDistance), distance0);
						if (newDistance < distance0) {
							distance0 = newDistance;
							closestHash = hash;
						}
						yPrimed += PrimeY;
					}
					xPrimed += PrimeX;
				}
				break;
			case Manhattan :
				for (int xi = xr - 1; xi <= xr + 1; xi++) {
					int yPrimed = yPrimedBase;

					for (int yi = yr - 1; yi <= yr + 1; yi++) {
						int hash = Hash(seed, xPrimed, yPrimed);
						int idx = hash & (255 << 1);

						float vecX = (float) (xi - x) + RandVecs2D[idx] * cellularJitter;
						float vecY = (float) (yi - y) + RandVecs2D[idx | 1] * cellularJitter;

						float newDistance = FastAbs(vecX) + FastAbs(vecY);

						distance1 = FastMax(FastMin(distance1, newDistance), distance0);
						if (newDistance < distance0) {
							distance0 = newDistance;
							closestHash = hash;
						}
						yPrimed += PrimeY;
					}
					xPrimed += PrimeX;
				}
				break;
			case Hybrid :
				for (int xi = xr - 1; xi <= xr + 1; xi++) {
					int yPrimed = yPrimedBase;

					for (int yi = yr - 1; yi <= yr + 1; yi++) {
						int hash = Hash(seed, xPrimed, yPrimed);
						int idx = hash & (255 << 1);

						float vecX = (float) (xi - x) + RandVecs2D[idx] * cellularJitter;
						float vecY = (float) (yi - y) + RandVecs2D[idx | 1] * cellularJitter;

						float newDistance = (FastAbs(vecX) + FastAbs(vecY)) + (vecX * vecX + vecY * vecY);

						distance1 = FastMax(FastMin(distance1, newDistance), distance0);
						if (newDistance < distance0) {
							distance0 = newDistance;
							closestHash = hash;
						}
						yPrimed += PrimeY;
					}
					xPrimed += PrimeX;
				}
				break;
		}

		if (mCellularDistanceFunction == CellularDistanceFunction.Euclidean && mCellularReturnType != CellularReturnType.CellValue) {
			distance0 = FastSqrt(distance0);

			if (mCellularReturnType != CellularReturnType.CellValue) {
				distance1 = FastSqrt(distance1);
			}
		}

		switch (mCellularReturnType) {
			case CellValue :
				return closestHash * (1 / 2147483648.0f);
			case Distance :
				return distance0 - 1;
			case Distance2 :
				return distance1 - 1;
			case Distance2Add :
				return (distance1 + distance0) * 0.5f - 1;
			case Distance2Sub :
				return distance1 - distance0 - 1;
			case Distance2Mul :
				return distance1 * distance0 * 0.5f - 1;
			case Distance2Div :
				return distance0 / distance1 - 1;
			default :
				return 0;
		}
	}

	// Perlin Noise

	private float SinglePerlin(int seed, float x, float y) {
		int x0 = FastFloor(x);
		int y0 = FastFloor(y);

		float xd0 = (float) (x - x0);
		float yd0 = (float) (y - y0);
		float xd1 = xd0 - 1;
		float yd1 = yd0 - 1;

		float xs = InterpQuintic(xd0);
		float ys = InterpQuintic(yd0);

		x0 *= PrimeX;
		y0 *= PrimeY;
		int x1 = x0 + PrimeX;
		int y1 = y0 + PrimeY;

		float xf0 = Lerp(GradCoord(seed, x0, y0, xd0, yd0), GradCoord(seed, x1, y0, xd1, yd0), xs);
		float xf1 = Lerp(GradCoord(seed, x0, y1, xd0, yd1), GradCoord(seed, x1, y1, xd1, yd1), xs);

		return Lerp(xf0, xf1, ys) * 1.4247691104677813f;
	}

	// Value Cubic Noise

	private float SingleValueCubic(int seed, float x, float y) {
		int x1 = FastFloor(x);
		int y1 = FastFloor(y);

		float xs = (float) (x - x1);
		float ys = (float) (y - y1);

		x1 *= PrimeX;
		y1 *= PrimeY;
		int x0 = x1 - PrimeX;
		int y0 = y1 - PrimeY;
		int x2 = x1 + PrimeX;
		int y2 = y1 + PrimeY;
		int x3 = x1 + (PrimeX << 1);
		int y3 = y1 + (PrimeY << 1);

		return CubicLerp(CubicLerp(ValCoord(seed, x0, y0), ValCoord(seed, x1, y0), ValCoord(seed, x2, y0), ValCoord(seed, x3, y0), xs),
				CubicLerp(ValCoord(seed, x0, y1), ValCoord(seed, x1, y1), ValCoord(seed, x2, y1), ValCoord(seed, x3, y1), xs),
				CubicLerp(ValCoord(seed, x0, y2), ValCoord(seed, x1, y2), ValCoord(seed, x2, y2), ValCoord(seed, x3, y2), xs),
				CubicLerp(ValCoord(seed, x0, y3), ValCoord(seed, x1, y3), ValCoord(seed, x2, y3), ValCoord(seed, x3, y3), xs), ys)
				* (1 / (1.5f * 1.5f));
	}

	// Value Noise

	private float SingleValue(int seed, float x, float y) {
		int x0 = FastFloor(x);
		int y0 = FastFloor(y);

		float xs = InterpHermite((float) (x - x0));
		float ys = InterpHermite((float) (y - y0));

		x0 *= PrimeX;
		y0 *= PrimeY;
		int x1 = x0 + PrimeX;
		int y1 = y0 + PrimeY;

		float xf0 = Lerp(ValCoord(seed, x0, y0), ValCoord(seed, x1, y0), xs);
		float xf1 = Lerp(ValCoord(seed, x0, y1), ValCoord(seed, x1, y1), xs);

		return Lerp(xf0, xf1, ys);
	}

	// Domain Warp

	private void DoSingleDomainWarp(int seed, float amp, float freq, float x, float y, Vector2 coord) {
		switch (mDomainWarpType) {
			case OpenSimplex2 :
				SingleDomainWarpSimplexGradient(seed, amp * 38.283687591552734375f, freq, x, y, coord, false);
				break;
			case OpenSimplex2Reduced :
				SingleDomainWarpSimplexGradient(seed, amp * 16.0f, freq, x, y, coord, true);
				break;
			case BasicGrid :
				SingleDomainWarpBasicGrid(seed, amp, freq, x, y, coord);
				break;
		}
	}

	// Domain Warp Single Wrapper

	private void DomainWarpSingle(Vector2 coord) {
		int seed = mSeed;
		float amp = mDomainWarpAmp * mFractalBounding;
		float freq = mFrequency;

		float xs = coord.x;
		float ys = coord.y;
		switch (mDomainWarpType) {
			case OpenSimplex2 :
			case OpenSimplex2Reduced : {
				float t = (xs + ys) * F2;
				xs += t;
				ys += t;
			}
				break;
			default :
				break;
		}

		DoSingleDomainWarp(seed, amp, freq, xs, ys, coord);
	}

	// Domain Warp Fractal Progressive

	private void DomainWarpFractalProgressive(Vector2 coord) {
		int seed = mSeed;
		float amp = mDomainWarpAmp * mFractalBounding;
		float freq = mFrequency;

		for (int i = 0; i < mOctaves; i++) {
			float xs = coord.x;
			float ys = coord.y;
			switch (mDomainWarpType) {
				case OpenSimplex2 :
				case OpenSimplex2Reduced : {
					float t = (xs + ys) * F2;
					xs += t;
					ys += t;
				}
					break;
				default :
					break;
			}

			DoSingleDomainWarp(seed, amp, freq, xs, ys, coord);

			seed++;
			amp *= mGain;
			freq *= mLacunarity;
		}
	}

	// Domain Warp Fractal Independant
	private void DomainWarpFractalIndependent(Vector2 coord) {
		float xs = coord.x;
		float ys = coord.y;
		switch (mDomainWarpType) {
			case OpenSimplex2 :
			case OpenSimplex2Reduced : {
				float t = (xs + ys) * F2;
				xs += t;
				ys += t;
			}
				break;
			default :
				break;
		}

		int seed = mSeed;
		float amp = mDomainWarpAmp * mFractalBounding;
		float freq = mFrequency;

		for (int i = 0; i < mOctaves; i++) {
			DoSingleDomainWarp(seed, amp, freq, xs, ys, coord);

			seed++;
			amp *= mGain;
			freq *= mLacunarity;
		}
	}

	// Domain Warp Basic Grid

	private void SingleDomainWarpBasicGrid(int seed, float warpAmp, float frequency, float x, float y, Vector2 coord) {
		float xf = x * frequency;
		float yf = y * frequency;

		int x0 = FastFloor(xf);
		int y0 = FastFloor(yf);

		float xs = InterpHermite((float) (xf - x0));
		float ys = InterpHermite((float) (yf - y0));

		x0 *= PrimeX;
		y0 *= PrimeY;
		int x1 = x0 + PrimeX;
		int y1 = y0 + PrimeY;

		int hash0 = Hash(seed, x0, y0) & (255 << 1);
		int hash1 = Hash(seed, x1, y0) & (255 << 1);

		float lx0x = Lerp(RandVecs2D[hash0], RandVecs2D[hash1], xs);
		float ly0x = Lerp(RandVecs2D[hash0 | 1], RandVecs2D[hash1 | 1], xs);

		hash0 = Hash(seed, x0, y1) & (255 << 1);
		hash1 = Hash(seed, x1, y1) & (255 << 1);

		float lx1x = Lerp(RandVecs2D[hash0], RandVecs2D[hash1], xs);
		float ly1x = Lerp(RandVecs2D[hash0 | 1], RandVecs2D[hash1 | 1], xs);

		coord.x += Lerp(lx0x, lx1x, ys) * warpAmp;
		coord.y += Lerp(ly0x, ly1x, ys) * warpAmp;
	}

	// Domain Warp Simplex/OpenSimplex2
	private void SingleDomainWarpSimplexGradient(int seed, float warpAmp, float frequency, float x, float y, Vector2 coord,
			boolean outGradOnly) {

		x *= frequency;
		y *= frequency;

		/*
		 * --- Skew moved to switch statements before fractal evaluation --- final
		 * FNLfloat F2 = 0.5f * (SQRT3 - 1); FNLfloat s = (x + y) * F2; x += s; y += s;
		 */

		int i = FastFloor(x);
		int j = FastFloor(y);
		float xi = (float) (x - i);
		float yi = (float) (y - j);

		float t = (xi + yi) * G2;
		float x0 = (float) (xi - t);
		float y0 = (float) (yi - t);

		i *= PrimeX;
		j *= PrimeY;

		float vx, vy;
		vx = vy = 0;

		float a = 0.5f - x0 * x0 - y0 * y0;
		if (a > 0) {
			float aaaa = (a * a) * (a * a);
			float xo, yo;
			if (outGradOnly) {
				int hash = Hash(seed, i, j) & (255 << 1);
				xo = RandVecs2D[hash];
				yo = RandVecs2D[hash | 1];
			} else {
				int hash = Hash(seed, i, j);
				int index1 = hash & (127 << 1);
				int index2 = (hash >> 7) & (255 << 1);
				float xg = Gradients2D[index1];
				float yg = Gradients2D[index1 | 1];
				float value = x0 * xg + y0 * yg;
				float xgo = RandVecs2D[index2];
				float ygo = RandVecs2D[index2 | 1];
				xo = value * xgo;
				yo = value * ygo;
			}
			vx += aaaa * xo;
			vy += aaaa * yo;
		}

		float c = (float) (2 * (1 - 2 * G2) * (1 / G2 - 2)) * t + ((float) (-2 * (1 - 2 * G2) * (1 - 2 * G2)) + a);
		if (c > 0) {
			float x2 = x0 + (2 * (float) G2 - 1);
			float y2 = y0 + (2 * (float) G2 - 1);
			float cccc = (c * c) * (c * c);
			float xo, yo;
			if (outGradOnly) {
				int hash = Hash(seed, i + PrimeX, j + PrimeY) & (255 << 1);
				xo = RandVecs2D[hash];
				yo = RandVecs2D[hash | 1];
			} else {
				int hash = Hash(seed, i + PrimeX, j + PrimeY);
				int index1 = hash & (127 << 1);
				int index2 = (hash >> 7) & (255 << 1);
				float xg = Gradients2D[index1];
				float yg = Gradients2D[index1 | 1];
				float value = x2 * xg + y2 * yg;
				float xgo = RandVecs2D[index2];
				float ygo = RandVecs2D[index2 | 1];
				xo = value * xgo;
				yo = value * ygo;
			}
			vx += cccc * xo;
			vy += cccc * yo;
		}

		if (y0 > x0) {
			float x1 = x0 + (float) G2;
			float y1 = y0 + ((float) G2 - 1);
			float b = 0.5f - x1 * x1 - y1 * y1;
			if (b > 0) {
				float bbbb = (b * b) * (b * b);
				float xo, yo;
				if (outGradOnly) {
					int hash = Hash(seed, i, j + PrimeY) & (255 << 1);
					xo = RandVecs2D[hash];
					yo = RandVecs2D[hash | 1];
				} else {
					int hash = Hash(seed, i, j + PrimeY);
					int index1 = hash & (127 << 1);
					int index2 = (hash >> 7) & (255 << 1);
					float xg = Gradients2D[index1];
					float yg = Gradients2D[index1 | 1];
					float value = x1 * xg + y1 * yg;
					float xgo = RandVecs2D[index2];
					float ygo = RandVecs2D[index2 | 1];
					xo = value * xgo;
					yo = value * ygo;
				}
				vx += bbbb * xo;
				vy += bbbb * yo;
			}
		} else {
			float x1 = x0 + ((float) G2 - 1);
			float y1 = y0 + (float) G2;
			float b = 0.5f - x1 * x1 - y1 * y1;
			if (b > 0) {
				float bbbb = (b * b) * (b * b);
				float xo, yo;
				if (outGradOnly) {
					int hash = Hash(seed, i + PrimeX, j) & (255 << 1);
					xo = RandVecs2D[hash];
					yo = RandVecs2D[hash | 1];
				} else {
					int hash = Hash(seed, i + PrimeX, j);
					int index1 = hash & (127 << 1);
					int index2 = (hash >> 7) & (255 << 1);
					float xg = Gradients2D[index1];
					float yg = Gradients2D[index1 | 1];
					float value = x1 * xg + y1 * yg;
					float xgo = RandVecs2D[index2];
					float ygo = RandVecs2D[index2 | 1];
					xo = value * xgo;
					yo = value * ygo;
				}
				vx += bbbb * xo;
				vy += bbbb * yo;
			}
		}

		coord.x += vx * warpAmp;
		coord.y += vy * warpAmp;
	}

	public static class Vector2 {
		public float x;
		public float y;

		public Vector2(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
}
