package retronism.tile;

import net.minecraft.src.*;
import aero.machineapi.Aero_IPort;
import aero.machineapi.Aero_IEnergyReceiver;

/**
 * AeroPort System: Redireciona tudo (Energia, Itens, Fluidos) para o Core.
 */
public class Retronism_TileMultiblockPort extends TileEntity implements Aero_IPort, Aero_IEnergyReceiver {
    
    public int coreX, coreY, coreZ;
    public boolean hasCore = false;
    public String type = "energy";

    public TileEntity getCore() {
        if (!hasCore) return null;
        TileEntity te = worldObj.getBlockTileEntity(coreX, coreY, coreZ);
        if (te == null) {
            hasCore = false;
            return null;
        }
        return te;
    }

    public void setCore(TileEntity core) {
        if (core != null) {
            this.coreX = core.xCoord;
            this.coreY = core.yCoord;
            this.coreZ = core.zCoord;
            this.hasCore = true;
        } else {
            this.hasCore = false;
        }
    }

    public String getPortType() { return type; }

    // --- Redirecionamento de Energia ---
    @Override
    public int receiveEnergy(int amount) {
        TileEntity core = getCore();
        if (core instanceof Aero_IEnergyReceiver) {
            return ((Aero_IEnergyReceiver) core).receiveEnergy(amount);
        }
        return 0;
    }

    @Override
    public int getStoredEnergy() {
        TileEntity core = getCore();
        if (core instanceof Aero_IEnergyReceiver) {
            return ((Aero_IEnergyReceiver) core).getStoredEnergy();
        }
        return 0;
    }

    @Override
    public int getMaxEnergy() {
        TileEntity core = getCore();
        if (core instanceof Aero_IEnergyReceiver) {
            return ((Aero_IEnergyReceiver) core).getMaxEnergy();
        }
        return 0;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        coreX = nbt.getInteger("cx");
        coreY = nbt.getInteger("cy");
        coreZ = nbt.getInteger("cz");
        hasCore = nbt.getBoolean("hc");
        type = nbt.getString("pt");
    }

    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("cx", coreX);
        nbt.setInteger("cy", coreY);
        nbt.setInteger("cz", coreZ);
        nbt.setBoolean("hc", hasCore);
        nbt.setString("pt", type);
    }
}
