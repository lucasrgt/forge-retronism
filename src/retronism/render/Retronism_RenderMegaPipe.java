package retronism.render;

import net.minecraft.src.*;
import aero.machineapi.*;
import retronism.block.*;

import org.lwjgl.opengl.GL11;


public class Retronism_RenderMegaPipe implements Retronism_IBlockRenderer {

	private static final int[] MEGA_COLORS = {0xD4AA00, 0x3366FF, 0xCCCCCC, 0xFF8800};
	private static final float[][] MEGA_TUBES = {
		{5.0F/16, 8.0F/16, 5.0F/16, 8.0F/16},   // energy
		{5.0F/16, 8.0F/16, 8.0F/16, 11.0F/16},  // fluid
		{8.0F/16, 11.0F/16, 5.0F/16, 8.0F/16},  // gas
		{8.0F/16, 11.0F/16, 8.0F/16, 11.0F/16}, // item
	};
	private static final int[] MEGA_TUBE_TYPES = {
		Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.TYPE_FLUID,
		Aero_SideConfig.TYPE_GAS, Aero_SideConfig.TYPE_ITEM
	};

	private void renderTubeSegment(RenderBlocks renderer, Block block, int x, int y, int z,
			float xMin, float yMin, float zMin, float xMax, float yMax, float zMax, int color) {
		block.setBlockBounds(xMin, yMin, zMin, xMax, yMax, zMax);
		renderer.overrideBlockTexture = 64;

		Tessellator t = Tessellator.instance;
		float brightness = block.getBlockBrightness(renderer.blockAccess, x, y, z);
		int cr = (color >> 16) & 0xFF;
		int cg = (color >> 8) & 0xFF;
		int cb = color & 0xFF;

		t.setColorOpaque((int)(cr * 0.5F * brightness), (int)(cg * 0.5F * brightness), (int)(cb * 0.5F * brightness));
		renderer.renderBottomFace(block, x, y, z, 64);
		t.setColorOpaque((int)(cr * brightness), (int)(cg * brightness), (int)(cb * brightness));
		renderer.renderTopFace(block, x, y, z, 64);
		t.setColorOpaque((int)(cr * 0.8F * brightness), (int)(cg * 0.8F * brightness), (int)(cb * 0.8F * brightness));
		renderer.renderEastFace(block, x, y, z, 64);
		renderer.renderWestFace(block, x, y, z, 64);
		t.setColorOpaque((int)(cr * 0.6F * brightness), (int)(cg * 0.6F * brightness), (int)(cb * 0.6F * brightness));
		renderer.renderNorthFace(block, x, y, z, 64);
		renderer.renderSouthFace(block, x, y, z, 64);

		renderer.overrideBlockTexture = -1;
	}

	public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		Retronism_BlockMegaPipe mega = (Retronism_BlockMegaPipe) block;

		boolean[] connected = new boolean[6];
		for (int side = 0; side < 6; side++) {
			int[] d = Retronism_RenderUtils.SIDE_OFFSETS[side];
			connected[side] = mega.canConnectTo(world, x+d[0], y+d[1], z+d[2]);
		}

		boolean[] sideHasArm = new boolean[6];

		for (int i = 0; i < 4; i++) {
			float tYMin = MEGA_TUBES[i][0], tYMax = MEGA_TUBES[i][1];
			float tZMin = MEGA_TUBES[i][2], tZMax = MEGA_TUBES[i][3];
			int color = MEGA_COLORS[i];
			int type = MEGA_TUBE_TYPES[i];

			// Center cube
			renderTubeSegment(renderer, block, x, y, z, tZMin, tYMin, tZMin, tZMax, tYMax, tZMax, color);

			for (int side = 0; side < 6; side++) {
				if (!connected[side]) continue;
				int mode = Retronism_RenderUtils.getPipeSideMode(world, x, y, z, side, type);
				if (mode == Aero_SideConfig.MODE_NONE) continue;
				int[] d = Retronism_RenderUtils.SIDE_OFFSETS[side];
				int oppSide = Aero_SideConfig.oppositeSide(side);
				int neighborMode = Retronism_RenderUtils.getNeighborSideMode(world, x+d[0], y+d[1], z+d[2], oppSide, type);
				if (neighborMode == Aero_SideConfig.MODE_NONE) continue;
				sideHasArm[side] = true;

				switch (side) {
					case 0: renderTubeSegment(renderer, block, x, y, z, tZMin, Retronism_RenderUtils.negBound(mode), tZMin, tZMax, tYMin, tZMax, color); break;
					case 1: renderTubeSegment(renderer, block, x, y, z, tZMin, tYMax, tZMin, tZMax, Retronism_RenderUtils.posBound(mode), tZMax, color); break;
					case 2: renderTubeSegment(renderer, block, x, y, z, tZMin, tYMin, Retronism_RenderUtils.negBound(mode), tZMax, tYMax, tZMin, color); break;
					case 3: renderTubeSegment(renderer, block, x, y, z, tZMin, tYMin, tZMax, tZMax, tYMax, Retronism_RenderUtils.posBound(mode), color); break;
					case 4: renderTubeSegment(renderer, block, x, y, z, Retronism_RenderUtils.negBound(mode), tYMin, tZMin, tZMin, tYMax, tZMax, color); break;
					case 5: renderTubeSegment(renderer, block, x, y, z, tZMax, tYMin, tZMin, Retronism_RenderUtils.posBound(mode), tYMax, tZMax, color); break;
				}
			}
		}

		// Connector plates on machine connections
		for (int side = 0; side < 6; side++) {
			if (!sideHasArm[side]) continue;
			int[] d = Retronism_RenderUtils.SIDE_OFFSETS[side];
			if (mega.isNeighborMachine(world, x+d[0], y+d[1], z+d[2])) {
				Retronism_RenderUtils.renderConnectorPlate(renderer, block, x, y, z, side, Retronism_RenderUtils.PIPE_PLATE_MIN, Retronism_RenderUtils.PIPE_PLATE_MAX, Retronism_RenderUtils.PLATE_THICK);
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
		Tessellator t = Tessellator.instance;
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

		for (int i = 0; i < 4; i++) {
			float tYMin = MEGA_TUBES[i][0], tYMax = MEGA_TUBES[i][1];
			float tZMin = MEGA_TUBES[i][2], tZMax = MEGA_TUBES[i][3];
			int color = MEGA_COLORS[i];

			block.setBlockBounds(2.0F/16, tYMin, tZMin, 14.0F/16, tYMax, tZMax);
			renderer.overrideBlockTexture = 64;

			t.startDrawingQuads(); t.setNormal(0,-1,0); t.setColorOpaque_I(color);
			renderer.renderBottomFace(block, 0, 0, 0, 64); t.draw();
			t.startDrawingQuads(); t.setNormal(0,1,0); t.setColorOpaque_I(color);
			renderer.renderTopFace(block, 0, 0, 0, 64); t.draw();
			t.startDrawingQuads(); t.setNormal(0,0,-1); t.setColorOpaque_I(color);
			renderer.renderEastFace(block, 0, 0, 0, 64); t.draw();
			t.startDrawingQuads(); t.setNormal(0,0,1); t.setColorOpaque_I(color);
			renderer.renderWestFace(block, 0, 0, 0, 64); t.draw();
			t.startDrawingQuads(); t.setNormal(-1,0,0); t.setColorOpaque_I(color);
			renderer.renderNorthFace(block, 0, 0, 0, 64); t.draw();
			t.startDrawingQuads(); t.setNormal(1,0,0); t.setColorOpaque_I(color);
			renderer.renderSouthFace(block, 0, 0, 0, 64); t.draw();

			renderer.overrideBlockTexture = -1;
		}

		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}
}
