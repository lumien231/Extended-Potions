package lumien.extendedpotions;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class SavingHandler
{
	public static NBTTagCompound writeCustomPotionEffectToNBT(PotionEffect pe, NBTTagCompound nbt)
	{
		nbt.setByte("Id", (byte) 0);
		nbt.setInteger("integerID", pe.getPotionID());
		nbt.setByte("Amplifier", (byte) pe.getAmplifier());
		nbt.setInteger("Duration", pe.getDuration());
		nbt.setBoolean("Ambient", pe.getIsAmbient());
		nbt.setBoolean("ShowParticles", pe.getIsShowParticles());
		return nbt;
	}

	public static PotionEffect readCustomPotionEffectFromNBT(NBTTagCompound nbt)
	{
		if (nbt.hasKey("integerID"))
		{
			int id = nbt.getInteger("integerID");

			if (id >= 0 && id < Potion.potionTypes.length && Potion.potionTypes[id] != null)
			{
				byte b1 = nbt.getByte("Amplifier");
				int i = nbt.getInteger("Duration");
				boolean flag = nbt.getBoolean("Ambient");
				boolean flag1 = true;

				if (nbt.hasKey("ShowParticles", 1))
				{
					flag1 = nbt.getBoolean("ShowParticles");
				}

				return new PotionEffect(id, i, b1, flag, flag1);
			}
			else
			{
				return null;
			}
		}
		else
		{
			byte b0 = nbt.getByte("Id");

			if (b0 >= 0 && b0 < Potion.potionTypes.length && Potion.potionTypes[b0] != null)
			{
				byte b1 = nbt.getByte("Amplifier");
				int i = nbt.getInteger("Duration");
				boolean flag = nbt.getBoolean("Ambient");
				boolean flag1 = true;

				if (nbt.hasKey("ShowParticles", 1))
				{
					flag1 = nbt.getBoolean("ShowParticles");
				}

				return new PotionEffect(b0, i, b1, flag, flag1);
			}
			else
			{
				return null;
			}
		}
	}
}
