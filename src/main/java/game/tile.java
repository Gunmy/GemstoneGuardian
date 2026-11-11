package game;

import javafx.scene.canvas.GraphicsContext;

import java.util.Random;

public class tile {
    private double realx, realy;
    private tileType tileType;
    private gameHandler handler;
    private int x, y;
    
    public tile (int x, int y, tileType tileType, int chunkx, int chunky, gameHandler handler) {
        if (x < 0 || y < 0) throw new IllegalArgumentException("x og y verdi kan ikke være under 0");
        this.tileType = tileType;
        this.realx = chunkx + ((double) x)/handler.getTileCount();
        this.realy = chunky + ((double) y)/handler.getTileCount();
        this.handler = handler;
        this.x = x + chunkx*handler.getTileCount();
        this.y = y + chunky*handler.getTileCount();
    }

    //xChange and yChange is the step taken to get out of the tile
    public boolean isBlockedOut(double xChange, double yChange) {
        return handler.collidersOn() && getTileType().isBlockedOut(xChange, yChange);
    }

    //xChange and yChange is the step taken to get into the tile
    public boolean isBlocked(double xChange, double yChange) {
        if (!handler.collidersOn()) {return false;}
        if (getTileType().isBlocked(xChange, yChange)) {return true;}

        for (character character : handler.getCharacters()) {
            if (character.isGone() || character.getLeader() != null) continue;
            if (character.getStandingTile() == this) {return true;}
            if (character.getTargetTile() == this)   {return true;}

        }

        return false;
    }

    public tileType getTileType() {return this.tileType;}

    //Where the tile is in the world, in chunks (the same coordinates characters use)
    public double getRealX () {return this.realx;}
    public double getRealY () {return this.realy;}

    private double getRenderVal (double realx, double realy) {
        double rendx = realx+0.5*handler.getUnitWidth()/handler.getTileCount();
        double rendy = realy+0.5*handler.getUnitHeight()/handler.getTileCount();

        return handler.getCamera().inRendered(rendx, rendy);
    }

    //Z = 1 and up, the ground is drawn by drawGround
    public void drawLayer (double realx, double realy, int z) {
        if (z > tileType.getMaxZ()) return;

        double renderVal = getRenderVal(realx, realy);
        if (renderVal > 0.01) {
            handler.getTempGc().setGlobalAlpha(renderVal);
            boolean flip = flipStart(realx);
            tileType.drawLayer(handler, realx, realy, x, y, z);
            if (flip) flipEnd();
            handler.getTempGc().setGlobalAlpha(1);
        }
    }

    //In a mirror world every tile is drawn flipped left to right round its own middle, and from the cold copy of the
    //sheet (imgDict.setCold). True when it is (the caller ends it with flipEnd after drawing the tile).
    //The middle is that of the whole pixels the tile types draw into: from (int) realx, one pixel wider than a tile so
    //it overlaps its neighbour on the right and no seam shows. Flipped round exactly that box it lands on itself, so the
    //overlap stays (flipped round the tile's own middle it moved left, and thin lines showed between the tiles)
    private boolean flipStart (double realx) {
        chunks world = handler.getWorld(handler.getCurrentWorld());
        if (world == null || !world.isMirror()) return false;
        GraphicsContext gc = handler.getTempGc();
        int left = (int) realx, width = (int) (handler.getUnitWidth()/handler.getTileCount() + 1);
        double middle = left + width/2.0;
        gc.save();
        gc.translate(middle, 0);
        gc.scale(-1, 1);
        gc.translate(-middle, 0);
        //Drawn from the cold copy of the sheet; the characters on it are not
        handler.getImages().setCold(true);
        return true;
    }

    private void flipEnd () {
        handler.getImages().setCold(false);
        handler.getTempGc().restore();
    }

    public void drawGround (double realx, double realy) {

        double renderVal = getRenderVal(realx, realy);
        if (renderVal > 0.01) {
            handler.getTempGc().setGlobalAlpha(renderVal);
            boolean flip = flipStart(realx);
            tileType.drawLayer(handler, realx, realy, x, y, 0);
            if (flip) flipEnd();
            //The particles this tile gives off are mirrored round its middle too (gameHandler.add*Particle)
            if (flip) handler.mirrorParticlesAround(this.realx + 0.5/handler.getTileCount());

            Random gen = new Random();
            double rdm = (gen).nextDouble();

            //Chests that will heal you when closed give off small green sparks, like the yellow ones events give off
            if (tileType.isContainer() && rdm < 0.2) {
                chest c = handler.chestOn(this);
                if (c != null && c.willHeal()) {
                    handler.addFrontParticle(
                        new coloredParticle(this.realx+0.055, this.realy+0.055, handler.getCurrentWorld(), 1, 1, 0.005, 0.005, 1, handler, (gen.nextDouble()-0.5)/3, (gen.nextDouble()-0.5)/3, "#21FF2C")
                    );
                }
            }

            switch(tileType.getId()) {
                case 45:
                    if (rdm < 0.05) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.075, this.realy, handler.getCurrentWorld(), 3, 1, 0.02, 0.02, 4, handler, 0, -0.2, "#E0D8D8")
                        );
                    }
                    break;
                case 48:
                    if (rdm < 0.02) {
                        handler.addBehindParticle(
                            new coloredParticle(this.realx+0.063, this.realy+0.05, handler.getCurrentWorld(), 1, 0, 0.015, 0.015, -0.5, handler, 0, 0.03, "#56CBDD")
                        );
                    }
                    break;
                case 0:
                    if (rdm < 0.001) {
                        handler.addBehindParticle(
                            new coloredParticle(this.realx+0.07, this.realy+0.03, handler.getCurrentWorld(), 1, 0.5, 0.02, 0.02, 0, handler, 0, 0, "#A0D0E0")
                        );
                    }
                    break;

                case 14:
                    if (rdm < 0.001) {
                        handler.addBehindParticle(
                            new coloredParticle(this.realx+0.07, this.realy+0.03, handler.getCurrentWorld(), 1, 0.5, 0.02, 0.02, 0, handler, 0, 0, "#A0D0E0")
                        );
                    }
                    break;

                case 24:
                if (rdm < 0.001) {
                    double rdm2 = gen.nextDouble()/100;
                    double rdm3 = gen.nextDouble()/100;
                    handler.addFrontParticle(
                        new coloredParticle(this.realx+rdm2*10, this.realy+rdm3*10, handler.getCurrentWorld(), 3, 1, 0.015+rdm2, 0.015+rdm2, 0, handler, 1, 0.5, "#1B895B")
                    );
                }
                break;

                //Volcano vent: smoke rising, and now and then an ember
                case 149:
                    if (rdm < 0.04) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.055, this.realy+0.04, handler.getCurrentWorld(), 2.5, 1, 0.02, 0.02, 3, handler, 0, -0.15, "#6E6E6E")
                        );
                    } else if (rdm < 0.05) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.045+gen.nextDouble()*0.02, this.realy+0.05, handler.getCurrentWorld(), 1.2, 0, 0.012, 0.012, 0, handler, (gen.nextDouble()-0.5)*0.05, -0.12, "#FC9F00")
                        );
                    }
                    break;

                //Sky portals: white and pale blue motes rising off them, plenty of them (only once lit)
                case 185: case 186: case 187:
                    if (rdm < 0.14 && (!(tileType instanceof skyPortalType) || ((skyPortalType) tileType).isLit())) {
                        String[] colors = {"#FFFFFF", "#F0F0F4", "#D8F4FF", "#8EDCF2"};
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+gen.nextDouble()*0.11, this.realy+0.02+gen.nextDouble()*0.09, handler.getCurrentWorld(), 1.4 + gen.nextDouble(), 1, 0.013, 0.013, -0.5, handler, (gen.nextDouble()-0.5)*0.04, -0.13, colors[gen.nextInt(colors.length)])
                        );
                    }
                    break;

                //Portals: purple sparks rising off them. The one in the girl's room only once she is beaten
                case 172: case 173: case 174: case 175: case 176: case 177: case 178: case 179: case 201: case 202: case 203:
                    if (rdm < 0.06 && (tileType.getId() != 172 || handler.portalLit())) {
                        String[] colors = {"#857BEF", "#887BFE", "#8EDCF2", "#FFFFFF"};
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+gen.nextDouble()*0.11, this.realy+0.02+gen.nextDouble()*0.09, handler.getCurrentWorld(), 1.2 + gen.nextDouble(), 1, 0.012, 0.012, -0.6, handler, (gen.nextDouble()-0.5)*0.03, -0.12, colors[gen.nextInt(colors.length)])
                        );
                    }
                    break;

                //Crystal floor: a rare blue glint
                case 160:
                    if (rdm < 0.002) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+gen.nextDouble()*0.11, this.realy+gen.nextDouble()*0.11, handler.getCurrentWorld(), 1, 1, 0.012, 0.012, -0.5, handler, 0, -0.02, "#8EDCF2")
                        );
                    }
                    break;

                case 91:
                    if (rdm < 0.01) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.055, this.realy+0.055, handler.getCurrentWorld(), 2, 0, 0.06, 0.06, -0.5, handler, 0, -0.15, "#21FF2C")
                        );
                    }
                    break;

                case 110:
                    if (rdm < 0.002) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.055+(-0.5+gen.nextDouble()), this.realy+0.055-1, handler.getCurrentWorld(), 3+gen.nextDouble(), 1, 0.015, 0.015, 0, handler, 0, 1, "#B2EFFF")
                        );
                    }
                    break;

                case 116:
                    if (rdm < 0.05) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.055, this.realy+0.015, handler.getCurrentWorld(), 3, 1, 0.01, 0.01, 5, handler, 0, -0.2, "#E0D8D8")
                        );
                    }
                    break;
                case 145:
                    if (rdm < 0.05) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.055, this.realy+0.015, handler.getCurrentWorld(), 3, 1, 0.01, 0.01, 5, handler, 0, -0.2, "#E0D8D8")
                        );
                    }
                    break;
                case 1:
                    if (rdm < 0.0001) {
                        double rdm2 = gen.nextDouble()/100;
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.055, this.realy+0.055, handler.getCurrentWorld(), 3, 1, 0.015+rdm2, 0.015+rdm2, 0, handler, 1, 0.5, "#1B895B")
                        );
                    }
                    break;

                case 127:
                    if (rdm < 0.05) {
                        double rdm2 = gen.nextDouble()/200;
                        handler.addBehindParticle(
                            new coloredParticle(this.realx+0.01+(gen.nextDouble()-0.5)/50, this.realy+0.11, handler.getCurrentWorld(), 3, 1, 0.01+rdm2, 0.01+rdm2, 1, handler, (gen.nextDouble()-0.5)/50, (gen.nextDouble()-0.5)/50, "#FFFFFF")
                        );
                    }


                    if (0.05 < rdm && rdm < 0.1) {
                        double rdm2 = gen.nextDouble()/300;
                        handler.addBehindParticle(
                            new coloredParticle(this.realx+0.01+(gen.nextDouble()-0.5)/50, this.realy+0.011, handler.getCurrentWorld(), 3, 1, 0.005+rdm2, 0.005+rdm2, 1, handler, (gen.nextDouble()-0.5)/50, (gen.nextDouble()-0.5)/50, "#FFFFFF")
                        );
                    }
                    break;

                case 137:
                    if (rdm < 0.05) {
                        handler.addFrontParticle(
                            new coloredParticle(this.realx+0.075, this.realy, handler.getCurrentWorld(), 3, 1, 0.02, 0.02, 4, handler, 0, -0.2, "#E0D8D8")
                        );
                    }
                    break;


                default:
                    break;
            }
            handler.mirrorParticlesAround(Double.NaN);
            handler.getTempGc().setGlobalAlpha(1);
        }
    }
}
