package retronism.block;

import net.minecraft.src.*;
import retronism.*;
import retronism.tile.*;
import retronism.gui.*;

import java.util.Random;

public class Retronism_BlockMegaCrusherCore extends BlockContainer {
	private Random rand = new Random();

	public Retronism_BlockMegaCrusherCore(int id, int textureIndex) {
		super(id, Material.iron);
		this.blockIndexInTexture = textureIndex;
	}

	public boolean isOpaqueCube() { return false; }
	public boolean renderAsNormalBlock() { return false; }
	public int getRenderType() { return mod_Retronism.megaCrusherRenderID; }

	public int idDropped(int metadata, Random random) {
		return Retronism_Registry.testBlock.blockID;
	}

	public int getBlockTextureFromSide(int side) {
		return side == 1 ? blockIndexInTexture + 17 : (side == 0 ? blockIndexInTexture + 17 : blockIndexInTexture);
	}

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		if (player.isSneaking()) return false;
		if (world.multiplayerWorld) return true;
		Retronism_TileMegaCrusher tile = (Retronism_TileMegaCrusher) world.getBlockTileEntity(x, y, z);
		ModLoader.OpenGUI(player, new Retronism_GuiMegaCrusher(player.inventory, tile));
		return true;
	}

	protected TileEntity getBlockEntity() {
		return new Retronism_TileMegaCrusher();
	}

	public void onBlockRemoval(World world, int x, int y, int z) {
		TileEntity te = world.getBlockTileEntity(x, y, z);
		if (te instanceof Retronism_TileMegaCrusher) {
			Retronism_TileMegaCrusher crusher = (Retronism_TileMegaCrusher) te;
			for (int i = 0; i < crusher.getSizeInventory(); ++i) {
				ItemStack stack = crusher.getStackInSlot(i);
				if (stack != null) {
					float rx = rand.nextFloat() * 0.8F + 0.1F;
					float ry = rand.nextFloat() * 0.8F + 0.1F;
					float rz = rand.nextFloat() * 0.8F + 0.1F;
					while (stack.stackSize > 0) {
						int dropCount = rand.nextInt(21) + 10;
						if (dropCount > stack.stackSize) dropCount = stack.stackSize;
						stack.stackSize -= dropCount;
						EntityItem entity = new EntityItem(world,
							(double)((float)x + rx), (double)((float)y + ry), (double)((float)z + rz),
							new ItemStack(stack.itemID, dropCount, stack.getItemDamage()));
						float spread = 0.05F;
						entity.motionX = (double)((float)rand.nextGaussian() * spread);
						entity.motionY = (double)((float)rand.nextGaussian() * spread + 0.2F);
						entity.motionZ = (double)((float)rand.nextGaussian() * spread);
						world.entityJoinedWorld(entity);
					}
				}
			}
		}
		super.onBlockRemoval(world, x, y, z);
	}
}
