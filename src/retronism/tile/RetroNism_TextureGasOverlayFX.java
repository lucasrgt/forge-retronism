package retronism.tile;

import net.minecraft.src.*;

public class Retronism_TextureGasOverlayFX extends TextureFX {
	private float[] field_g = new float[256];
	private float[] field_h = new float[256];
	private float[] field_i = new float[256];
	private float[] field_j = new float[256];

	public Retronism_TextureGasOverlayFX(int slot) {
		super(slot);
	}

	public void onTick() {
		for (int x = 0; x < 16; ++x) {
			for (int y = 0; y < 16; ++y) {
				float v = 0.0F;
				for (int xx = x - 1; xx <= x + 1; ++xx) {
					v += this.field_g[(xx & 15) + (y & 15) * 16];
				}
				this.field_h[x + y * 16] = v / 3.3F + this.field_i[x + y * 16] * 0.8F;
			}
		}

		for (int x = 0; x < 16; ++x) {
			for (int y = 0; y < 16; ++y) {
				this.field_i[x + y * 16] += this.field_j[x + y * 16] * 0.05F;
				if (this.field_i[x + y * 16] < 0.0F) {
					this.field_i[x + y * 16] = 0.0F;
				}
				this.field_j[x + y * 16] -= 0.1F;
				if (Math.random() < 0.05D) {
					this.field_j[x + y * 16] = 0.5F;
				}
			}
		}

		float[] tmp = this.field_h;
		this.field_h = this.field_g;
		this.field_g = tmp;

		for (int i = 0; i < 256; ++i) {
			float v = this.field_g[i];
			if (v > 1.0F) v = 1.0F;
			if (v < 0.0F) v = 0.0F;
			float v2 = v * v;
			int gray = (int)(130.0F + v2 * 80.0F);
			int alpha = 255;
			this.imageData[i * 4 + 0] = (byte) gray;
			this.imageData[i * 4 + 1] = (byte) gray;
			this.imageData[i * 4 + 2] = (byte) gray;
			this.imageData[i * 4 + 3] = (byte) alpha;
		}
	}
}
