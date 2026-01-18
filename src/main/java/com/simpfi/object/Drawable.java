package com.simpfi.object;

import java.awt.Color;
import java.awt.Graphics2D;
/** Interface used to draw elements of the map. */
public interface Drawable {

	public void draw(Graphics2D g, Color c, SimulationSnapshot snapshot);

}
