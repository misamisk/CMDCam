package com.creativemd.cmdcam;

import org.lwjgl.opengl.GL11;

import com.creativemd.cmdcam.key.KeyHandler;
import com.creativemd.cmdcam.movement.Movement;
import com.creativemd.cmdcam.movement.OutsidePath;
import com.creativemd.cmdcam.utils.CamPoint;
import com.creativemd.cmdcam.utils.interpolation.CosineInterpolation;
import com.creativemd.cmdcam.utils.interpolation.CubicInterpolation;
import com.creativemd.cmdcam.utils.interpolation.HermiteInterpolation;
import com.creativemd.cmdcam.utils.interpolation.Interpolation;
import com.creativemd.cmdcam.utils.interpolation.LinearInterpolation;
import com.creativemd.cmdcam.utils.interpolation.Vec3;
import com.creativemd.creativecore.client.rendering.RenderHelper3D;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class CamEventHandler {

	public static Minecraft mc = Minecraft.getMinecraft();
	public static float defaultfov = 70.0F;
	public static final float amountZoom = 0.1F;
	public static final float amountroll = 0.5F;
	
	public static boolean selectEntityMode = false;
	
	public static long lastRenderTime;
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if(!selectEntityMode)
			return ;
		
		if(event instanceof EntityInteract)
		{
			CMDCam.target = ((EntityInteract) event).getTarget();
			event.getEntityPlayer().addChatMessage(new TextComponentString("Target is set to " + ((EntityInteract) event).getTarget().getCachedUniqueIdString() + "."));
			selectEntityMode = false;
		}
		
		if(event instanceof RightClickBlock)
		{
			CMDCam.target = event.getPos();
			event.getEntityPlayer().addChatMessage(new TextComponentString("Target is set to " +  event.getPos() + "."));
			selectEntityMode = false;
		}
	}
	
	@SubscribeEvent
	public void onRenderTick(RenderTickEvent event)
	{
		if(mc.thePlayer != null && mc.theWorld != null)
		{
			if(mc.inGameHasFocus) //&& event.phase == Phase.START)
			{
				if(CMDCam.currentPath == null)
				{
					if(mc.gameSettings.isKeyDown(KeyHandler.zoomIn))
					{
						if(mc.thePlayer.isSneaking())
							mc.gameSettings.fovSetting -= amountZoom*10;
						else
							mc.gameSettings.fovSetting -= amountZoom;
					}
					
					if(mc.gameSettings.isKeyDown(KeyHandler.zoomOut))
					{
						if(mc.thePlayer.isSneaking())
							mc.gameSettings.fovSetting += amountZoom*10;
						else
							mc.gameSettings.fovSetting += amountZoom;
					}
					
					if(mc.gameSettings.isKeyDown(KeyHandler.zoomCenter))
					{
						mc.gameSettings.fovSetting = defaultfov;
					}
					CMDCam.fov = mc.gameSettings.fovSetting;
					
					if(mc.gameSettings.isKeyDown(KeyHandler.rollLeft))
						CMDCam.roll -= amountroll;
					
					if(mc.gameSettings.isKeyDown(KeyHandler.rollRight))
						CMDCam.roll += amountroll;
					
					if(mc.gameSettings.isKeyDown(KeyHandler.rollCenter))
						CMDCam.roll = 0;
					
					if(KeyHandler.pointKey.isPressed())
					{
						CMDCam.points.add(new CamPoint());
						mc.thePlayer.addChatMessage(new TextComponentString("Registered " + CMDCam.points.size() + ". Point!"));
					}
					
					
				}else{
					CMDCam.currentPath.tick(event.renderTickTime);
				}
				
				//ReflectionHelper.setPrivateValue(EntityRenderer.class, mc.entityRenderer, CMDCam.roll, "camRoll", "field_78495_O"); 
				if(KeyHandler.startStop.isPressed())
				{
					if(CMDCam.currentPath != null)
					{
						CMDCam.currentPath.onPathFinished();
						CMDCam.currentPath = null;
					}
					else
						CMDCam.createPath();
				}
			} 
		}
		lastRenderTime = System.nanoTime();
	}
	
	@SubscribeEvent
	public void worldRender(RenderWorldLastEvent event)
	{
		boolean shouldRender = false;
		for (Movement movement : Movement.movements.values()) {
			if(movement.isRenderingEnabled)
			{
				shouldRender = true;
				break;
			}
		}
		if(CMDCam.currentPath == null && shouldRender && CMDCam.points.size() > 2)
		{
			GlStateManager.enableBlend();
	        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
	        GlStateManager.disableTexture2D();
	        GL11.glDepthMask(false);
	        
			Vec3[] points = new Vec3[CMDCam.points.size()];
			for (int i = 0; i < points.length; i++) {
				points[i] = new Vec3(CMDCam.points.get(i).x, CMDCam.points.get(i).y, CMDCam.points.get(i).z);
				GlStateManager.pushMatrix();
				GlStateManager.translate(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY+mc.thePlayer.getEyeHeight()-0.1, -TileEntityRendererDispatcher.staticPlayerZ);
				RenderHelper3D.renderBlock(points[i].x, points[i].y, points[i].z, 0.1, 0.1, 0.1, 0, 0, 0, 1, 1, 1, 1);
				float f = TileEntityRendererDispatcher.instance.entityYaw;
	            float f1 = TileEntityRendererDispatcher.instance.entityPitch;
	            boolean flag = false;
	            EntityRenderer.func_189692_a(mc.fontRendererObj, (i+1) + "", (float)points[i].x, (float)points[i].y + 0.4F, (float)points[i].z, 0, f, f1, false, false);
	            GL11.glDepthMask(false);
	            GlStateManager.disableLighting();
	            GlStateManager.disableTexture2D();
	            GlStateManager.popMatrix();
			}
	        
			if(Movement.hermite.isRenderingEnabled)
		        renderInterpolation(new HermiteInterpolation<>(points), new Vec3(1,1,1));
	        
			if(Movement.cubic.isRenderingEnabled)
		        renderInterpolation(new CubicInterpolation<>(points), new Vec3(1,0,0));
			
			if(Movement.cosine.isRenderingEnabled)
		        renderInterpolation(new CosineInterpolation<>(points), new Vec3(0,1,0));
			
			if(Movement.linear.isRenderingEnabled)
		        renderInterpolation(new LinearInterpolation<>(points), new Vec3(0,0,1));
	        
			GL11.glDepthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.resetColor();
            
			
		}
	}
	
	public void renderInterpolation(Interpolation<Vec3> interpolation, Vec3 color)
	{
		double steps = 20*(interpolation.points.size()-1);
        
		GlStateManager.pushMatrix();
		GL11.glColor3d(color.x, color.y, color.z);
		GlStateManager.translate(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY+mc.thePlayer.getEyeHeight()-0.1, -TileEntityRendererDispatcher.staticPlayerZ);
		GlStateManager.glLineWidth(1.0F);
		GlStateManager.glBegin(GL11.GL_LINE_STRIP);
		for (int i = 0; i < steps; i++) {
			double t = i/(double)steps;
			//System.out.println("t=" + t);
			Vec3 pos = interpolation.valueAt(t);
			GL11.glVertex3d(pos.x, pos.y, pos.z);
		}
		Vec3 last = interpolation.points.get(interpolation.points.size()-1);
		GL11.glVertex3d(last.x, last.y, last.z);
		GlStateManager.glEnd();
		GlStateManager.popMatrix();
	}
	
	public static boolean shouldPlayerTakeInput()
	{
		return true;
	}
	
	public Entity renderEntity;
	
	@SubscribeEvent
	public void renderPlayerPre(RenderPlayerEvent.Pre event)
	{
		if(CMDCam.currentPath instanceof OutsidePath)
		{
			renderEntity = mc.getRenderManager().renderViewEntity;
			
			mc.getRenderManager().renderViewEntity = mc.thePlayer;
		}
	}
	
	@SubscribeEvent
	public void renderPlayerPost(RenderPlayerEvent.Post event)
	{
		if(CMDCam.currentPath instanceof OutsidePath)
		{
			mc.getRenderManager().renderViewEntity = renderEntity;
		}
	}
	
	@SubscribeEvent
	public void cameraRoll(CameraSetup event)
	{
		event.setRoll(CMDCam.roll);
	}
	
}
