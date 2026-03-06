package retronism.item;

import net.minecraft.src.*;

public class Retronism_ItemTest extends Item {

	public String getModName() { return "Retronism"; }

	public Retronism_ItemTest(int id) {
		super(id);
		this.maxStackSize = 64;
	}
}
