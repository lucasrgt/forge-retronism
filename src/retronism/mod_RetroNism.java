package retronism;

import net.minecraft.src.*;
import retronism.render.*;
import retronism.tile.*;
import net.minecraft.client.Minecraft;
import java.util.HashMap;

public class mod_Retronism extends BaseMod {

	public static int cableRenderID;
	public static int fluidPipeRenderID;
	public static int gasPipeRenderID;
	public static int megaPipeRenderID;
	public static int itemPipeRenderID;
	public static int crusherRenderID;
	public static int machinePortRenderID;
	public static int megaCrusherRenderID;
	public static final int GAS_OVERLAY_INDEX = 175;

	public static int texCrusher;
	public static int texMegaCrusherModel;

	private HashMap renderers = new HashMap();

	public mod_Retronism() {
		// Render IDs
		cableRenderID = ModLoader.getUniqueBlockModelID(this, true);
		fluidPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);
		gasPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);
		megaPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);
		itemPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);
		crusherRenderID = ModLoader.getUniqueBlockModelID(this, true);
		machinePortRenderID = ModLoader.getUniqueBlockModelID(this, true);
		megaCrusherRenderID = ModLoader.getUniqueBlockModelID(this, true);

		// Texture overrides
		Retronism_Registry.wrench.setIconIndex(ModLoader.addOverride("/gui/items.png", "/item/retronism_wrench.png"));
		texCrusher = ModLoader.addOverride("/terrain.png", "/block/retronism_crusher.png");
		Retronism_Registry.crusherBlock.blockIndexInTexture = texCrusher;
		Retronism_Registry.machinePortBlock.texEnergy = ModLoader.addOverride("/terrain.png", "/block/retronism_port_energy.png");
		Retronism_Registry.machinePortBlock.texFluid = ModLoader.addOverride("/terrain.png", "/block/retronism_port_fluid.png");
		Retronism_Registry.machinePortBlock.texGas = ModLoader.addOverride("/terrain.png", "/block/retronism_port_gas.png");
		texMegaCrusherModel = ModLoader.addOverride("/terrain.png", "/models/retronism_megacrusher.png");

		// Register blocks, tiles, names
		Retronism_Registry.registerAll(this);

		// Register recipes
		Retronism_Recipes.registerAll();

		// Register renderers (DI pattern)
		renderers.put(new Integer(cableRenderID), new Retronism_RenderCable());
		renderers.put(new Integer(fluidPipeRenderID), new Retronism_RenderFluidPipe());
		renderers.put(new Integer(gasPipeRenderID), new Retronism_RenderGasPipe());
		renderers.put(new Integer(megaPipeRenderID), new Retronism_RenderMegaPipe());
		renderers.put(new Integer(itemPipeRenderID), new Retronism_RenderItemPipe());
		renderers.put(new Integer(crusherRenderID), new Retronism_RenderCrusher());
		renderers.put(new Integer(machinePortRenderID), new Retronism_RenderMachinePort());
		renderers.put(new Integer(megaCrusherRenderID), new Retronism_RenderMegaCrusher());
	}

	public boolean RenderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block, int modelID) {
		Retronism_IBlockRenderer r = (Retronism_IBlockRenderer) renderers.get(new Integer(modelID));
		return r != null ? r.renderWorld(renderer, world, x, y, z, block) : false;
	}

	public void RenderInvBlock(RenderBlocks renderer, Block block, int metadata, int modelID) {
		Retronism_IBlockRenderer r = (Retronism_IBlockRenderer) renderers.get(new Integer(modelID));
		if (r != null) r.renderInventory(renderer, block, metadata);
	}

	public void RegisterAnimation(Minecraft game) {
		ModLoader.addAnimation(new Retronism_TextureGasOverlayFX(GAS_OVERLAY_INDEX));
	}

	public String Version() {
		return "0.1.0";
	}
}
