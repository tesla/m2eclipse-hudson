/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.api;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;

/**
 * Hudson related images to be shared..
 * @author mkleint
 *
 */
public final class HudsonImages
{
  private HudsonImages() {
      
  }

  public static final String HUDSON = com.sonatype.buildserver.eclipse.ui.HudsonImages.HUDSON;
  
  public static final String OVERLAY_CHANGED = com.sonatype.buildserver.eclipse.ui.HudsonImages.OVERLAY_CHANGED;
  
  public static Image getHudsonImage() {
      return com.sonatype.buildserver.eclipse.ui.HudsonImages.getImage( com.sonatype.buildserver.eclipse.ui.HudsonImages.HUDSON );
  }
  
  public static Image getOverlayImage(String base, String overlay, int quadrant) {
	  assert quadrant == IDecoration.TOP_RIGHT || quadrant == IDecoration.BOTTOM_RIGHT : "only botton_right and top_right supported";
	  return com.sonatype.buildserver.eclipse.ui.HudsonImages.getOverlayImage(base, 
			  quadrant == IDecoration.TOP_RIGHT ? overlay : null,
			  quadrant == IDecoration.BOTTOM_RIGHT ? overlay : null);
  }
}
