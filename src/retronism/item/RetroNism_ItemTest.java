package retronism.item;

import net.minecraft.src.*;

public class RetroNism_ItemTest extends Item {

	public String getModName() { return "RetroNism"; }

	public RetroNism_ItemTest(int id) {
		super(id);
		this.maxStackSize = 64;
	}
}
