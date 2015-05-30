package lumien.extendedpotions.asm;

import static org.objectweb.asm.Opcodes.*;
import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ClassTransformer implements IClassTransformer
{
	Logger logger = LogManager.getLogger("ExtendedPotionsCore");

	public ClassTransformer()
	{
		logger.log(Level.DEBUG, "Starting Class Transformation");
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (transformedName.equals("net.minecraft.network.play.server.S1DPacketEntityEffect"))
		{
			return patchEffectPacket(basicClass);
		}
		else if (transformedName.equals("net.minecraft.network.play.server.S1EPacketRemoveEntityEffect"))
		{
			return patchRemoveEffectPacket(basicClass);
		}
		else if (transformedName.equals("net.minecraft.client.network.NetHandlerPlayClient"))
		{
			return patchClientNetHandlerClass(basicClass);
		}
		else if (transformedName.equals("net.minecraft.potion.PotionEffect"))
		{
			return patchPotionEffect(basicClass);
		}
		else if (transformedName.equals("net.minecraft.potion.Potion"))
		{
			return patchPotion(basicClass);
		}
		return basicClass;
	}

	private byte[] patchPotion(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.DEBUG, "Found Potion Class: " + classNode.name);

		MethodNode constructor = null;

		for (MethodNode mn : classNode.methods)
		{
			if (mn.name.equals("<init>") && mn.desc.equals("(ILnet/minecraft/util/ResourceLocation;ZI)V"))
			{
				constructor = mn;
				break;
			}
		}

		if (constructor != null)
		{
			for (int i=0;i<constructor.instructions.size();i++)
			{
				AbstractInsnNode ain = constructor.instructions.get(i);
				
				if (ain instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) ain;
					if (min.getOpcode()==INVOKESPECIAL && min.owner.equals("java/lang/Object") && min.name.equals("<init>") && min.desc.equals("()V"))
					{
						logger.log(Level.DEBUG, " - Found Constructor");
						InsnList toInsert = new InsnList();
						toInsert.add(new VarInsnNode(ALOAD, 0));
						toInsert.add(new VarInsnNode(ILOAD, 1));
						toInsert.add(new MethodInsnNode(INVOKESTATIC, "lumien/extendedpotions/ErrorHandler", "checkID", "(Lnet/minecraft/potion/Potion;I)V", false));
						constructor.instructions.insert(min,toInsert);
					}
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	private byte[] patchPotionEffect(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.DEBUG, "Found PotionEffect Class: " + classNode.name);

		MethodNode readCustomPotionEffectFromNBT = null;
		MethodNode writeCustomPotionEffectToNBT = null;

		for (MethodNode mn : classNode.methods)
		{
			if (mn.name.equals(MCPNames.method("func_82722_b")))
			{
				readCustomPotionEffectFromNBT = mn;
			}
			else if (mn.name.equals(MCPNames.method("func_82719_a")))
			{
				writeCustomPotionEffectToNBT = mn;
			}
		}

		if (readCustomPotionEffectFromNBT != null)
		{
			logger.log(Level.DEBUG, " - Found readCustomPotionEffectFromNBT");

			InsnList toInsert = new InsnList();

			toInsert.add(new VarInsnNode(ALOAD, 0));
			toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "lumien/extendedpotions/SavingHandler", "readCustomPotionEffectFromNBT", "(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/potion/PotionEffect;", false));
			toInsert.add(new InsnNode(Opcodes.ARETURN));

			readCustomPotionEffectFromNBT.instructions.insert(toInsert);
		}

		if (writeCustomPotionEffectToNBT != null)
		{
			logger.log(Level.DEBUG, " - Found writeCustomPotionEffectToNBT");

			InsnList toInsert = new InsnList();

			toInsert.add(new VarInsnNode(ALOAD, 0));
			toInsert.add(new VarInsnNode(ALOAD, 1));
			toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "lumien/extendedpotions/SavingHandler", "writeCustomPotionEffectToNBT", "(Lnet/minecraft/potion/PotionEffect;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagCompound;", false));
			toInsert.add(new InsnNode(Opcodes.ARETURN));

			writeCustomPotionEffectToNBT.instructions.insert(toInsert);
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	private byte[] patchClientNetHandlerClass(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.DEBUG, "Found NetHandlerPlayClient Class: " + classNode.name);

		MethodNode handleEffect = null;

		for (MethodNode mn : classNode.methods)
		{
			if (mn.name.equals(MCPNames.method("func_147260_a")))
			{
				handleEffect = mn;
			}
		}

		if (handleEffect != null)
		{
			logger.log(Level.DEBUG, " - Found handleEffect");
			for (int i = 0; i < handleEffect.instructions.size(); i++)
			{
				AbstractInsnNode ain = handleEffect.instructions.get(i);

				if (ain instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) ain;

					if (min.owner.equals("net/minecraft/network/play/server/S1DPacketEntityEffect") && min.name.equals("func_149427_e"))
					{
						logger.log(Level.DEBUG, " - Changed handleEffect to refer to proper method");

						min.desc = "()I";
					}
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	private byte[] patchEffectPacket(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.DEBUG, "Found S1DPacketEntityEffect Class: " + classNode.name);

		for (FieldNode fn : classNode.fields)
		{
			if (fn.name.equals("field_149432_b"))
			{
				logger.log(Level.DEBUG, " - Changed description of field_149432_b from B to I");
				fn.desc = "I";
			}
		}

		MethodNode constructor = null;
		MethodNode getId = null;

		MethodNode readPacketData = null;
		MethodNode writePacketData = null;

		for (MethodNode mn : classNode.methods)
		{
			if (mn.name.equals("<init>") && mn.desc.equals("(ILnet/minecraft/potion/PotionEffect;)V"))
			{
				constructor = mn;
			}
			else if (mn.name.equals("func_149427_e"))
			{
				getId = mn;
			}
			else if (mn.name.equals(MCPNames.method("func_148837_a")))
			{
				readPacketData = mn;
			}
			else if (mn.name.equals(MCPNames.method("func_148840_b")))
			{
				writePacketData = mn;
			}
		}

		if (getId != null)
		{
			logger.log(Level.DEBUG, " - Changed getId to Integer");
			getId.desc = "()I";
			for (AbstractInsnNode ain : getId.instructions.toArray())
			{
				if (ain instanceof FieldInsnNode)
				{
					FieldInsnNode fin = (FieldInsnNode) ain;

					fin.desc = "I";
				}
			}
		}

		if (constructor != null)
		{
			for (int i = 0; i < constructor.instructions.size(); i++)
			{
				AbstractInsnNode ain = constructor.instructions.get(i);

				if (ain instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) ain;

					if (min.name.equals(MCPNames.method("func_76456_a")))
					{
						logger.log(Level.DEBUG, " - Changed Constructor to properly save in field");
						for (int c = 0; c < 3; c++)
						{
							AbstractInsnNode n = constructor.instructions.get(i + 1);
							constructor.instructions.remove(n);
						}
					}
				}
				else if (ain instanceof FieldInsnNode)
				{
					FieldInsnNode fin = (FieldInsnNode) ain;

					if (fin.name.equals("field_149432_b"))
					{
						fin.desc = "I";
					}
				}
			}
		}

		if (readPacketData != null)
		{
			logger.log(Level.DEBUG, " - Patching readPacketData");

			for (int i = 0; i < readPacketData.instructions.size(); i++)
			{
				AbstractInsnNode ain = readPacketData.instructions.get(i);

				if (ain instanceof FieldInsnNode)
				{
					FieldInsnNode fin = (FieldInsnNode) ain;

					if (fin.name.equals("field_149432_b"))
					{
						fin.desc = "I";
					}
				}

				if (ain instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) ain;

					if (min.owner.equals("net/minecraft/network/PacketBuffer"))
					{
						if (min.name.equals("readByte") && readPacketData.instructions.get(i + 1) instanceof FieldInsnNode)
						{
							FieldInsnNode fin = (FieldInsnNode) readPacketData.instructions.get(i + 1);
							if (fin.name.equals("field_149432_b"))
							{
								logger.log(Level.DEBUG, " - Changed reading to readInt");

								MethodInsnNode newMethod = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/network/PacketBuffer", "readInt", "()I", false);
								readPacketData.instructions.insert(min, newMethod);
								readPacketData.instructions.remove(min);
							}
						}
					}
				}
			}
		}

		if (writePacketData != null)
		{
			logger.log(Level.DEBUG, " - Patching writePacketData");

			for (int i = 0; i < writePacketData.instructions.size(); i++)
			{
				AbstractInsnNode ain = writePacketData.instructions.get(i);

				if (ain instanceof FieldInsnNode)
				{
					FieldInsnNode fin = (FieldInsnNode) ain;

					if (fin.name.equals("field_149432_b"))
					{
						fin.desc = "I";
					}
				}

				if (ain instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) ain;

					if (min.owner.equals("net/minecraft/network/PacketBuffer"))
					{
						if (min.name.equals("writeByte") && writePacketData.instructions.get(i - 1) instanceof FieldInsnNode)
						{
							FieldInsnNode fin = (FieldInsnNode) writePacketData.instructions.get(i - 1);
							if (fin.name.equals("field_149432_b"))
							{
								logger.log(Level.DEBUG, " - Changed writing to writeInt");
								MethodInsnNode newMethod = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/network/PacketBuffer", "writeInt", "(I)Lio/netty/buffer/ByteBuf;", false);

								writePacketData.instructions.insert(min, newMethod);
								writePacketData.instructions.remove(min);
							}
						}
					}
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	private byte[] patchRemoveEffectPacket(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.DEBUG, "Found S1EPacketRemoveEntityEffect Class: " + classNode.name);

		MethodNode constructor = null;
		MethodNode getId = null;

		MethodNode readPacketData = null;
		MethodNode writePacketData = null;

		for (MethodNode mn : classNode.methods)
		{
			if (mn.name.equals("<init>") && mn.desc.equals("(ILnet/minecraft/potion/PotionEffect;)V"))
			{
				constructor = mn;
			}
			else if (mn.name.equals("func_149075_d"))
			{
				getId = mn;
			}
			else if (mn.name.equals(MCPNames.method("func_148837_a")))
			{
				readPacketData = mn;
			}
			else if (mn.name.equals(MCPNames.method("func_148840_b")))
			{
				writePacketData = mn;
			}
		}

		if (getId != null)
		{
			logger.log(Level.DEBUG, " - Changed getId to Integer");
			getId.desc = "()I";
			for (AbstractInsnNode ain : getId.instructions.toArray())
			{
				if (ain instanceof FieldInsnNode)
				{
					FieldInsnNode fin = (FieldInsnNode) ain;

					fin.desc = "I";
				}
			}
		}

		if (constructor != null)
		{
			for (int i = 0; i < constructor.instructions.size(); i++)
			{
				AbstractInsnNode ain = constructor.instructions.get(i);

				if (ain instanceof FieldInsnNode)
				{
					FieldInsnNode fin = (FieldInsnNode) ain;

					if (fin.name.equals("field_149078_b"))
					{
						fin.desc = "I";
					}
				}
			}
		}

		if (readPacketData != null)
		{
			logger.log(Level.DEBUG, " - Patching readPacketData");

			for (int i = 0; i < readPacketData.instructions.size(); i++)
			{
				AbstractInsnNode ain = readPacketData.instructions.get(i);

				if (ain instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) ain;

					if (min.owner.equals("net/minecraft/network/PacketBuffer"))
					{
						if (min.name.equals("readUnsignedByte") && readPacketData.instructions.get(i + 1) instanceof FieldInsnNode)
						{
							FieldInsnNode fin = (FieldInsnNode) readPacketData.instructions.get(i + 1);
							if (fin.name.equals("field_149078_b"))
							{
								logger.log(Level.DEBUG, " - Changed reading to readInt");

								MethodInsnNode newMethod = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/network/PacketBuffer", "readInt", "()I", false);
								readPacketData.instructions.insert(min, newMethod);
								readPacketData.instructions.remove(min);
							}
						}
					}
				}
			}
		}

		if (writePacketData != null)
		{
			logger.log(Level.DEBUG, " - Patching writePacketData");

			for (int i = 0; i < writePacketData.instructions.size(); i++)
			{
				AbstractInsnNode ain = writePacketData.instructions.get(i);

				if (ain instanceof MethodInsnNode)
				{
					MethodInsnNode min = (MethodInsnNode) ain;

					if (min.owner.equals("net/minecraft/network/PacketBuffer"))
					{
						if (min.name.equals("writeByte") && writePacketData.instructions.get(i - 1) instanceof FieldInsnNode)
						{
							FieldInsnNode fin = (FieldInsnNode) writePacketData.instructions.get(i - 1);
							if (fin.name.equals("field_149078_b"))
							{
								logger.log(Level.DEBUG, " - Changed writing to writeInt");
								MethodInsnNode newMethod = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/network/PacketBuffer", "writeInt", "(I)Lio/netty/buffer/ByteBuf;", false);

								writePacketData.instructions.insert(min, newMethod);
								writePacketData.instructions.remove(min);
							}
						}
					}
				}
			}
		}

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);

		return writer.toByteArray();
	}

	private byte[] patchDummyClass(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		logger.log(Level.DEBUG, "Found Dummy Class: " + classNode.name);

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);

		return writer.toByteArray();
	}
}
