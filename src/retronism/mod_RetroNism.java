package retronism;

import net.minecraft.src.*;
import retronism.api.*;
import retronism.block.*;
import retronism.tile.*;
import retronism.item.*;
import retronism.recipe.*;
import org.lwjgl.opengl.GL11;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;

public class mod_RetroNism extends BaseMod {

	public static int cableRenderID;
	public static int fluidPipeRenderID;
	public static int gasPipeRenderID;
	public static int megaPipeRenderID;
	public static final int GAS_OVERLAY_INDEX = 175;

	public static final Block testBlock = (new RetroNism_BlockTest(200, 1))
		.setHardness(3.0F)
		.setResistance(5.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismTest");

	public static final Block cableBlock = (new RetroNism_BlockCable(201, 22))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismCable");

	public static final Block crusherBlock = (new RetroNism_BlockCrusher(202, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismCrusher");

	public static final Block generatorBlock = (new RetroNism_BlockGenerator(203, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismGenerator");

	public static final Block pumpBlock = (new RetroNism_BlockPump(204, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismPump");

	public static final Block fluidPipeBlock = (new RetroNism_BlockFluidPipe(205, 23))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismFluidPipe");

	public static final Block electrolysisBlock = (new RetroNism_BlockElectrolysis(206, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismElectrolysis");

	public static final Block gasPipeBlock = (new RetroNism_BlockGasPipe(207, 54))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismGasPipe");

	public static final Block fluidTankBlock = (new RetroNism_BlockFluidTank(208, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismFluidTank");

	public static final Block gasTankBlock = (new RetroNism_BlockGasTank(209, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismGasTank");

	public static final Block megaPipeBlock = (new RetroNism_BlockMegaPipe(210, 22))
		.setBlockName("retroNismMegaPipe");

	public static final Item testItem = (new RetroNism_ItemTest(500))
		.setIconIndex(7 + 3 * 16)
		.setItemName("retroNismTestItem");

	public static final Item ironDust = (new RetroNism_ItemDust(501))
		.setIconIndex(13 + 1 * 16)
		.setItemName("retroNismIronDust");

	public static final Item goldDust = (new RetroNism_ItemDust(502))
		.setIconIndex(13 + 2 * 16)
		.setItemName("retroNismGoldDust");

	public static final Item diamondDust = (new RetroNism_ItemDust(503))
		.setIconIndex(13 + 3 * 16)
		.setItemName("retroNismDiamondDust");

	public static final Item obsidianDust = (new RetroNism_ItemDust(504))
		.setIconIndex(13 + 4 * 16)
		.setItemName("retroNismObsidianDust");

	public static final Item gasCellEmpty = (new RetroNism_ItemGasCell(505))
		.setIconIndex(10 + 6 * 16)
		.setItemName("retroNismGasCellEmpty");

	public static final Item gasCellHydrogen = (new RetroNism_ItemGasCell(506))
		.setIconIndex(11 + 6 * 16)
		.setItemName("retroNismGasCellHydrogen");

	public static final Item gasCellOxygen = (new RetroNism_ItemGasCell(507))
		.setIconIndex(12 + 6 * 16)
		.setItemName("retroNismGasCellOxygen");

	public static final Item wrench = (new RetroNism_ItemWrench(508))
		.setIconIndex(10 + 1 * 16)
		.setItemName("retroNismWrench");

	public mod_RetroNism() {
		cableRenderID = ModLoader.getUniqueBlockModelID(this, true);
		fluidPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);
		gasPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);
		megaPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);

		ModLoader.RegisterBlock(testBlock);
		ModLoader.RegisterBlock(cableBlock);
		ModLoader.RegisterBlock(crusherBlock);
		ModLoader.RegisterBlock(generatorBlock);
		ModLoader.RegisterBlock(pumpBlock);
		ModLoader.RegisterBlock(fluidPipeBlock);
		ModLoader.RegisterBlock(electrolysisBlock);
		ModLoader.RegisterBlock(gasPipeBlock);
		ModLoader.RegisterBlock(fluidTankBlock);
		ModLoader.RegisterBlock(gasTankBlock);
		ModLoader.RegisterBlock(megaPipeBlock);
		ModLoader.RegisterTileEntity(RetroNism_TileCrusher.class, "Crusher");
		ModLoader.RegisterTileEntity(RetroNism_TileGenerator.class, "Generator");
		ModLoader.RegisterTileEntity(RetroNism_TileCable.class, "Cable");
		ModLoader.RegisterTileEntity(RetroNism_TilePump.class, "Pump");
		ModLoader.RegisterTileEntity(RetroNism_TileFluidPipe.class, "FluidPipe");
		ModLoader.RegisterTileEntity(RetroNism_TileElectrolysis.class, "Electrolysis");
		ModLoader.RegisterTileEntity(RetroNism_TileGasPipe.class, "GasPipe");
		ModLoader.RegisterTileEntity(RetroNism_TileFluidTank.class, "FluidTank");
		ModLoader.RegisterTileEntity(RetroNism_TileGasTank.class, "GasTank");
		ModLoader.RegisterTileEntity(RetroNism_TileMegaPipe.class, "MegaPipe");
		ModLoader.AddName(testBlock, "RetroNism Test Block");
		ModLoader.AddName(cableBlock, "RetroNism Cable");
		ModLoader.AddName(crusherBlock, "Crusher");
		ModLoader.AddName(generatorBlock, "Generator");
		ModLoader.AddName(pumpBlock, "Water Pump");
		ModLoader.AddName(fluidPipeBlock, "Fluid Pipe");
		ModLoader.AddName(electrolysisBlock, "Electrolysis Machine");
		ModLoader.AddName(gasPipeBlock, "Gas Pipe");
		ModLoader.AddName(fluidTankBlock, "Fluid Tank");
		ModLoader.AddName(gasTankBlock, "Gas Tank");
		ModLoader.AddName(megaPipeBlock, "Mega Pipe");
		ModLoader.AddName(testItem, "RetroNism Test Item");
		ModLoader.AddName(ironDust, "Iron Dust");
		ModLoader.AddName(goldDust, "Gold Dust");
		ModLoader.AddName(diamondDust, "Diamond Dust");
		ModLoader.AddName(obsidianDust, "Obsidian Dust");
		ModLoader.AddName(gasCellEmpty, "Empty Gas Cell");
		ModLoader.AddName(gasCellHydrogen, "Hydrogen Gas Cell");
		ModLoader.AddName(gasCellOxygen, "Oxygen Gas Cell");
		ModLoader.AddName(wrench, "Wrench");


		// Debug recipes (easy craft for testing)
		ModLoader.AddRecipe(
			new ItemStack(testBlock, 64),
			new Object[] {
				"D",
				'D', Block.dirt
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(cableBlock, 16),
			new Object[] {
				"S",
				'S', Block.sand
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(testItem, 64),
			new Object[] {
				"C",
				'C', Block.cobblestone
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(crusherBlock, 1),
			new Object[] {
				"G",
				'G', Block.gravel
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(generatorBlock, 1),
			new Object[] {
				"N",
				'N', Block.netherrack
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(pumpBlock, 1),
			new Object[] {
				"L",
				'L', Block.blockClay
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(fluidPipeBlock, 16),
			new Object[] {
				"A",
				'A', Block.glass
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(electrolysisBlock, 1),
			new Object[] {
				"W",
				'W', Item.lightStoneDust
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(gasPipeBlock, 16),
			new Object[] {
				"R",
				'R', Item.redstone
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(fluidTankBlock, 1),
			new Object[] {
				"I",
				'I', Block.blockSteel
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(gasTankBlock, 1),
			new Object[] {
				"O",
				'O', Block.obsidian
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(gasCellEmpty, 1),
			new Object[] {
				"G",
				'G', Block.glass
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(megaPipeBlock, 16),
			new Object[] {
				"D",
				'D', Item.diamond
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(wrench, 1),
			new Object[] {
				"S",
				'S', Item.stick
			}
		);

		// Crusher recipes: ore -> 2 dust
		RetroNism_RecipesCrusher.crushing().addCrushing(Block.oreIron.blockID, new ItemStack(ironDust, 2));
		RetroNism_RecipesCrusher.crushing().addCrushing(Block.oreGold.blockID, new ItemStack(goldDust, 2));
		// Crusher recipes: material -> dust
		RetroNism_RecipesCrusher.crushing().addCrushing(Item.diamond.shiftedIndex, new ItemStack(diamondDust, 2));
		RetroNism_RecipesCrusher.crushing().addCrushing(Block.obsidian.blockID, new ItemStack(obsidianDust, 2));

		// Smelting recipes: dust -> ingot
		FurnaceRecipes.smelting().addSmelting(ironDust.shiftedIndex, new ItemStack(Item.ingotIron));
		FurnaceRecipes.smelting().addSmelting(goldDust.shiftedIndex, new ItemStack(Item.ingotGold));
	}

	public boolean RenderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block, int modelID) {
		if(modelID == cableRenderID) {
			return renderCable(renderer, world, x, y, z, block);
		}
		if(modelID == fluidPipeRenderID) {
			return renderFluidPipe(renderer, world, x, y, z, block);
		}
		if(modelID == gasPipeRenderID) {
			return renderGasPipe(renderer, world, x, y, z, block);
		}
		if(modelID == megaPipeRenderID) {
			return renderMegaPipe(renderer, world, x, y, z, block);
		}
		return false;
	}

	public void RenderInvBlock(RenderBlocks renderer, Block block, int metadata, int modelID) {
		if(modelID == megaPipeRenderID) {
			renderMegaPipeInv(renderer, block);
			return;
		}
		if(modelID == cableRenderID || modelID == fluidPipeRenderID || modelID == gasPipeRenderID) {
			float bMin = (modelID == cableRenderID) ? 6.0F/16 : 5.0F/16;
			float bMax = (modelID == cableRenderID) ? 10.0F/16 : 11.0F/16;
			block.setBlockBounds(2.0F/16, bMin, bMin, 14.0F/16, bMax, bMax);
			Tessellator tessellator = Tessellator.instance;
			GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
			tessellator.startDrawingQuads();
			tessellator.setNormal(0.0F, -1.0F, 0.0F);
			renderer.renderBottomFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(0));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(0.0F, 1.0F, 0.0F);
			renderer.renderTopFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(1));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(0.0F, 0.0F, -1.0F);
			renderer.renderEastFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(2));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(0.0F, 0.0F, 1.0F);
			renderer.renderWestFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(3));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(-1.0F, 0.0F, 0.0F);
			renderer.renderNorthFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(4));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(1.0F, 0.0F, 0.0F);
			renderer.renderSouthFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(5));
			tessellator.draw();
			GL11.glTranslatef(0.5F, 0.5F, 0.5F);
			block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	// Arm offset: INPUT recedes 2px, OUTPUT extends 2px, I/O normal, NONE no arm
	private static final float INSET = 2.0F / 16.0F;

	private static int getPipeSideMode(IBlockAccess world, int x, int y, int z, int side, int type) {
		TileEntity te = world.getBlockTileEntity(x, y, z);
		if (te instanceof RetroNism_ISideConfigurable) {
			return RetroNism_SideConfig.get(((RetroNism_ISideConfigurable) te).getSideConfig(), side, type);
		}
		return RetroNism_SideConfig.MODE_INPUT_OUTPUT;
	}

	private boolean renderCable(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 6.0F / 16.0F;
		float max = 10.0F / 16.0F;
		RetroNism_BlockCable cable = (RetroNism_BlockCable) block;
		int E = RetroNism_SideConfig.TYPE_ENERGY;

		// Center piece
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		// Down (-Y) = SIDE_BOTTOM(0)
		if(cable.canConnectTo(world, x, y - 1, z)) {
			int mode = getPipeSideMode(world, x, y, z, 0, E);
			if (mode != RetroNism_SideConfig.MODE_NONE) {
				float end = (mode == RetroNism_SideConfig.MODE_INPUT) ? INSET : (mode == RetroNism_SideConfig.MODE_OUTPUT) ? -INSET : 0.0F;
				block.setBlockBounds(min, end, min, max, min, max);
				renderer.renderStandardBlock(block, x, y, z);
			}
		}
		// Up (+Y) = SIDE_TOP(1)
		if(cable.canConnectTo(world, x, y + 1, z)) {
			int mode = getPipeSideMode(world, x, y, z, 1, E);
			if (mode != RetroNism_SideConfig.MODE_NONE) {
				float end = (mode == RetroNism_SideConfig.MODE_INPUT) ? 1.0F - INSET : (mode == RetroNism_SideConfig.MODE_OUTPUT) ? 1.0F + INSET : 1.0F;
				block.setBlockBounds(min, max, min, max, end, max);
				renderer.renderStandardBlock(block, x, y, z);
			}
		}
		// North (-Z) = SIDE_NORTH(2)
		if(cable.canConnectTo(world, x, y, z - 1)) {
			int mode = getPipeSideMode(world, x, y, z, 2, E);
			if (mode != RetroNism_SideConfig.MODE_NONE) {
				float end = (mode == RetroNism_SideConfig.MODE_INPUT) ? INSET : (mode == RetroNism_SideConfig.MODE_OUTPUT) ? -INSET : 0.0F;
				block.setBlockBounds(min, min, end, max, max, min);
				renderer.renderStandardBlock(block, x, y, z);
			}
		}
		// South (+Z) = SIDE_SOUTH(3)
		if(cable.canConnectTo(world, x, y, z + 1)) {
			int mode = getPipeSideMode(world, x, y, z, 3, E);
			if (mode != RetroNism_SideConfig.MODE_NONE) {
				float end = (mode == RetroNism_SideConfig.MODE_INPUT) ? 1.0F - INSET : (mode == RetroNism_SideConfig.MODE_OUTPUT) ? 1.0F + INSET : 1.0F;
				block.setBlockBounds(min, min, max, max, max, end);
				renderer.renderStandardBlock(block, x, y, z);
			}
		}
		// West (-X) = SIDE_WEST(4)
		if(cable.canConnectTo(world, x - 1, y, z)) {
			int mode = getPipeSideMode(world, x, y, z, 4, E);
			if (mode != RetroNism_SideConfig.MODE_NONE) {
				float end = (mode == RetroNism_SideConfig.MODE_INPUT) ? INSET : (mode == RetroNism_SideConfig.MODE_OUTPUT) ? -INSET : 0.0F;
				block.setBlockBounds(end, min, min, min, max, max);
				renderer.renderStandardBlock(block, x, y, z);
			}
		}
		// East (+X) = SIDE_EAST(5)
		if(cable.canConnectTo(world, x + 1, y, z)) {
			int mode = getPipeSideMode(world, x, y, z, 5, E);
			if (mode != RetroNism_SideConfig.MODE_NONE) {
				float end = (mode == RetroNism_SideConfig.MODE_INPUT) ? 1.0F - INSET : (mode == RetroNism_SideConfig.MODE_OUTPUT) ? 1.0F + INSET : 1.0F;
				block.setBlockBounds(max, min, min, end, max, max);
				renderer.renderStandardBlock(block, x, y, z);
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	private static float negBound(int mode) {
		if (mode == RetroNism_SideConfig.MODE_INPUT) return INSET;
		if (mode == RetroNism_SideConfig.MODE_OUTPUT) return -INSET;
		return 0.0F;
	}
	private static float posBound(int mode) {
		if (mode == RetroNism_SideConfig.MODE_INPUT) return 1.0F - INSET;
		if (mode == RetroNism_SideConfig.MODE_OUTPUT) return 1.0F + INSET;
		return 1.0F;
	}

	private void renderPipeArm(RenderBlocks renderer, Block block, int x, int y, int z,
			int side, int mode, float min, float max, float iMin, float iMax,
			float fillRatio, int fluidTex, float eps) {
		if (mode == RetroNism_SideConfig.MODE_NONE) return;
		boolean hasFluid = fillRatio > 0 && fluidTex >= 0;
		float fillTop = iMin + (iMax - iMin) * fillRatio;

		switch (side) {
			case 0: { // Down (-Y)
				float end = negBound(mode);
				block.setBlockBounds(min, end, min, max, min, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					float downBot = iMin - (iMin - end) * fillRatio;
					block.setBlockBounds(iMin-eps, downBot, iMin-eps, iMax+eps, iMin+eps, iMax+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 1: { // Up (+Y)
				float end = posBound(mode);
				block.setBlockBounds(min, max, min, max, end, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					float upTop = iMax + ((end - iMax) * fillRatio);
					block.setBlockBounds(iMin-eps, iMax-eps, iMin-eps, iMax+eps, upTop, iMax+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 2: { // North (-Z)
				float end = negBound(mode);
				block.setBlockBounds(min, min, end, max, max, min);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					block.setBlockBounds(iMin-eps, iMin, end-eps, iMax+eps, fillTop, iMin+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 3: { // South (+Z)
				float end = posBound(mode);
				block.setBlockBounds(min, min, max, max, max, end);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					block.setBlockBounds(iMin-eps, iMin, iMax-eps, iMax+eps, fillTop, end+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 4: { // West (-X)
				float end = negBound(mode);
				block.setBlockBounds(end, min, min, min, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					block.setBlockBounds(end-eps, iMin, iMin-eps, iMin+eps, fillTop, iMax+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 5: { // East (+X)
				float end = posBound(mode);
				block.setBlockBounds(max, min, min, end, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					block.setBlockBounds(iMax-eps, iMin, iMin-eps, end+eps, fillTop, iMax+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
		}
	}

	// Neighbor positions matching SideConfig: Bottom, Top, North, South, West, East
	private static final int[][] SIDE_OFFSETS = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};

	private boolean renderFluidPipe(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float iMin = 6.0F / 16.0F;
		float iMax = 10.0F / 16.0F;
		float eps = 0.002F;
		RetroNism_BlockFluidPipe pipe = (RetroNism_BlockFluidPipe) block;
		int F = RetroNism_SideConfig.TYPE_FLUID;

		TileEntity te = world.getBlockTileEntity(x, y, z);
		float fillRatio = 0.0F;
		int fluidTex = -1;
		if (te instanceof RetroNism_TileFluidPipe) {
			RetroNism_TileFluidPipe tilePipe = (RetroNism_TileFluidPipe) te;
			if (tilePipe.getFluidAmount() > 0) {
				float visualCapacity = 200.0F;
				fillRatio = Math.min(1.0F, (float) tilePipe.getFluidAmount() / visualCapacity);
				fluidTex = Block.waterMoving.blockIndexInTexture;
			}
		}
		if (fillRatio > 0.0F && fillRatio < 0.18F) fillRatio = 0.18F;

		// Center shell
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		// Center fluid fill
		if (fillRatio > 0 && fluidTex >= 0) {
			float fillTop = iMin + (iMax - iMin) * fillRatio;
			block.setBlockBounds(iMin - eps, iMin, iMin - eps, iMax + eps, fillTop, iMax + eps);
			renderer.overrideBlockTexture = fluidTex;
			renderer.renderStandardBlock(block, x, y, z);
			renderer.overrideBlockTexture = -1;
		}

		// Arms with side config
		for (int side = 0; side < 6; side++) {
			int[] d = SIDE_OFFSETS[side];
			if (pipe.canConnectTo(world, x+d[0], y+d[1], z+d[2])) {
				int mode = getPipeSideMode(world, x, y, z, side, F);
				renderPipeArm(renderer, block, x, y, z, side, mode, min, max, iMin, iMax, fillRatio, fluidTex, eps);
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	private void renderGasArm(RenderBlocks renderer, Block block, int x, int y, int z,
			int side, int mode, float min, float max, float iMin, float iMax, boolean hasGas, int gasColor) {
		if (mode == RetroNism_SideConfig.MODE_NONE) return;
		switch (side) {
			case 0: {
				float end = negBound(mode);
				block.setBlockBounds(min, end, min, max, min, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(iMin, end, iMin, iMax, iMin, iMax); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 1: {
				float end = posBound(mode);
				block.setBlockBounds(min, max, min, max, end, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(iMin, max, iMin, iMax, end, iMax); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 2: {
				float end = negBound(mode);
				block.setBlockBounds(min, min, end, max, max, min);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(iMin, iMin, end, iMax, iMax, iMin); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 3: {
				float end = posBound(mode);
				block.setBlockBounds(min, min, max, max, max, end);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(iMin, iMin, max, iMax, iMax, end); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 4: {
				float end = negBound(mode);
				block.setBlockBounds(end, min, min, min, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(end, iMin, iMin, iMin, iMax, iMax); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 5: {
				float end = posBound(mode);
				block.setBlockBounds(max, min, min, end, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(max, iMin, iMin, end, iMax, iMax); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
		}
	}

	private boolean renderGasPipe(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float iMin = 6.0F / 16.0F;
		float iMax = 10.0F / 16.0F;
		RetroNism_BlockGasPipe pipe = (RetroNism_BlockGasPipe) block;
		int G = RetroNism_SideConfig.TYPE_GAS;

		TileEntity te = world.getBlockTileEntity(x, y, z);
		boolean hasGas = false;
		int gasColor = 0xFFFFFFFF;
		if (te instanceof RetroNism_TileGasPipe) {
			RetroNism_TileGasPipe tilePipe = (RetroNism_TileGasPipe) te;
			if (tilePipe.getGasAmount() > 0) {
				hasGas = true;
				gasColor = RetroNism_GasType.getColor(tilePipe.getGasType());
			}
		}

		// Center shell
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		if (hasGas) {
			block.setBlockBounds(iMin, iMin, iMin, iMax, iMax, iMax);
			renderer.overrideBlockTexture = 64;
			Tessellator.instance.setColorOpaque_I(gasColor);
			renderer.renderStandardBlock(block, x, y, z);
			renderer.overrideBlockTexture = -1;
		}

		for (int side = 0; side < 6; side++) {
			int[] d = SIDE_OFFSETS[side];
			if (pipe.canConnectTo(world, x+d[0], y+d[1], z+d[2])) {
				int mode = getPipeSideMode(world, x, y, z, side, G);
				renderGasArm(renderer, block, x, y, z, side, mode, min, max, iMin, iMax, hasGas, gasColor);
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	// ========== MEGA PIPE RENDER ==========
	// Bundled cable style: 4 colored tubes (2x2, no gap) that all extend together
	// Energy(yellow): Y=5-8, Z=5-8    Fluid(blue):  Y=5-8, Z=8-11
	// Gas(gray):      Y=8-11, Z=5-8   Item(orange): Y=8-11, Z=8-11

	private static final int[] MEGA_COLORS = {0xD4AA00, 0x3366FF, 0xCCCCCC, 0xFF8800};
	private static final float[][] MEGA_TUBES = {
		{5.0F/16, 8.0F/16, 5.0F/16, 8.0F/16},   // energy
		{5.0F/16, 8.0F/16, 8.0F/16, 11.0F/16},  // fluid
		{8.0F/16, 11.0F/16, 5.0F/16, 8.0F/16},  // gas
		{8.0F/16, 11.0F/16, 8.0F/16, 11.0F/16}, // item
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

	// MEGA_TUBE_TYPES maps tube index to SideConfig type: 0=energy, 1=fluid, 2=gas, 3=item
	private static final int[] MEGA_TUBE_TYPES = {
		RetroNism_SideConfig.TYPE_ENERGY, RetroNism_SideConfig.TYPE_FLUID,
		RetroNism_SideConfig.TYPE_GAS, RetroNism_SideConfig.TYPE_ITEM
	};

	private boolean renderMegaPipe(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		RetroNism_BlockMegaPipe mega = (RetroNism_BlockMegaPipe) block;

		boolean[] connected = new boolean[6];
		for (int side = 0; side < 6; side++) {
			int[] d = SIDE_OFFSETS[side];
			connected[side] = mega.canConnectTo(world, x+d[0], y+d[1], z+d[2]);
		}

		for (int i = 0; i < 4; i++) {
			float tYMin = MEGA_TUBES[i][0], tYMax = MEGA_TUBES[i][1];
			float tZMin = MEGA_TUBES[i][2], tZMax = MEGA_TUBES[i][3];
			int color = MEGA_COLORS[i];
			int type = MEGA_TUBE_TYPES[i];

			// Center cube
			renderTubeSegment(renderer, block, x, y, z, tZMin, tYMin, tZMin, tZMax, tYMax, tZMax, color);

			for (int side = 0; side < 6; side++) {
				if (!connected[side]) continue;
				int mode = getPipeSideMode(world, x, y, z, side, type);
				if (mode == RetroNism_SideConfig.MODE_NONE) continue;

				switch (side) {
					case 0: { // Down
						float end = negBound(mode);
						renderTubeSegment(renderer, block, x, y, z, tZMin, end, tZMin, tZMax, tYMin, tZMax, color);
						break;
					}
					case 1: { // Up
						float end = posBound(mode);
						renderTubeSegment(renderer, block, x, y, z, tZMin, tYMax, tZMin, tZMax, end, tZMax, color);
						break;
					}
					case 2: { // North
						float end = negBound(mode);
						renderTubeSegment(renderer, block, x, y, z, tZMin, tYMin, end, tZMax, tYMax, tZMin, color);
						break;
					}
					case 3: { // South
						float end = posBound(mode);
						renderTubeSegment(renderer, block, x, y, z, tZMin, tYMin, tZMax, tZMax, tYMax, end, color);
						break;
					}
					case 4: { // West
						float end = negBound(mode);
						renderTubeSegment(renderer, block, x, y, z, end, tYMin, tZMin, tZMin, tYMax, tZMax, color);
						break;
					}
					case 5: { // East
						float end = posBound(mode);
						renderTubeSegment(renderer, block, x, y, z, tZMax, tYMin, tZMin, end, tYMax, tZMax, color);
						break;
					}
				}
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	private void renderMegaPipeInv(RenderBlocks renderer, Block block) {
		Tessellator t = Tessellator.instance;
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

		for (int i = 0; i < 4; i++) {
			float tYMin = MEGA_TUBES[i][0], tYMax = MEGA_TUBES[i][1];
			float tZMin = MEGA_TUBES[i][2], tZMax = MEGA_TUBES[i][3];
			int color = MEGA_COLORS[i];

			// Inventory shows a short segment along X
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

	public void RegisterAnimation(Minecraft game) {
		ModLoader.addAnimation(new RetroNism_TextureGasOverlayFX(GAS_OVERLAY_INDEX));
	}

	public String Version() {
		return "0.1.0";
	}
}
