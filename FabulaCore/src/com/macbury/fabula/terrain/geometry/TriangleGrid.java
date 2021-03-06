package com.macbury.fabula.terrain.geometry;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class TriangleGrid implements Disposable {
  public static enum AttributeType {
    Position, Normal, Color, TextureCord, TilePosition, Passable
  }
  
  public static final int VERTEXT_PER_COL        = 4;
  private int rows;
  private int columns;
  private short vertexCursor;
  private short vertexIndex;
  private short indicesCursor;
  private ArrayList<GridVertex> vertexsList;
  private ArrayList<AttributeType> attributeTypes;
  
  private float[] verties;
  private short[] indices;
  private Mesh mesh;
  private int vertextCount;
  private GridVertex currentVertex;
  private boolean started = false;
  
  public TriangleGrid(int width, int height, boolean isStatic) {
    this.rows           = height;
    this.columns        = width;
    
    this.vertexsList    = new ArrayList<GridVertex>(rows*columns);
    this.attributeTypes = new ArrayList<AttributeType>();
  }
  
  public void using(AttributeType type) {
    if (!isUsing(type)) {
      this.attributeTypes.add(type);
    }
  }
  
  public boolean isUsing(AttributeType type) {
    return (this.attributeTypes.indexOf(type) >= 0);
  }
  
  public int getAttributesPerVertex() {
    int count = 0;
    if (isUsing(AttributeType.Position)) {
      count+=3;
    }
    
    if (isUsing(AttributeType.Normal)) {
      count+=3;
    }
    
    if (isUsing(AttributeType.TextureCord)) {
      count+=2;
    }
    
    if (isUsing(AttributeType.TilePosition)) {
      count+=2;
    }
    
    if (isUsing(AttributeType.Color)) {
      count++;
    }
    return count;
  }
  
  public void calculateNormals() {
    //Gdx.app.log("Debug", "Indices: " + indices.length + " Vertex: " + vertexsList.size());
    if (indices.length <= 3) {
      return;
    }
    for (int i = 0; i < indices.length / 3; i++) {
      int index1 = indices[i * 3];
      int index2 = indices[i * 3 + 1];
      int index3 = indices[i * 3 + 2];
      
      Vector3 side1   = this.vertexsList.get(index1).position.cpy().sub(this.vertexsList.get(index3).position);
      Vector3 side2   = this.vertexsList.get(index1).position.cpy().sub(this.vertexsList.get(index2).position);
      Vector3 normal  = side1.crs(side2);
      
      this.vertexsList.get(index1).normal.add(normal);
      this.vertexsList.get(index2).normal.add(normal);
      this.vertexsList.get(index3).normal.add(normal);
    }
  }
  
  public int getVertexSize() {
    return getAttributesPerVertex();
  }
  
  public void begin() {
    if (this.started) {
      throw new GdxRuntimeException("Already started building geometry! Call end() first!");
    }
    
    clear();
    this.indices = new short[vertextCount * 3];
    this.verties = null;
  }
  
  public void clear() {
    this.vertexCursor  = 0;
    this.indicesCursor = 0;
    this.vertexIndex   = 0;

    this.vertextCount   = rows*columns*getAttributesPerVertex();
    this.attributeTypes.clear();
    this.vertexsList.clear();
  }
  
  public short addVertex(float x, float y, float z) {
    currentVertex = new GridVertex();
    currentVertex.position.set(x, y, z);
    using(AttributeType.Position);
    this.vertexsList.add(currentVertex);
    this.started       = true;
    return vertexIndex++;
  }
  
  public void addNormal() {
    this.addNormal(0.0f,0.0f,0.0f);
    using(AttributeType.Normal);
  }

  public void addNormal(float x, float y, float z) {
    currentVertex.normal.set(x, y, z);
    using(AttributeType.Normal);
  }

  public void addTilePos(float x, float z) {
    using(AttributeType.TilePosition);
    currentVertex.tilePosition.set(x, z);
  }
  
  public void addColorToVertex(float r, float g, float b, float a) {
    using(AttributeType.Color);
    currentVertex.color.set(r, g, b, a);
  }
  
  public void addPassableInfo(boolean passable) {
    //using(AttributeType.Passable);
    //currentVertex.passable = passable;
  }
  
  public void addUVMap(float u, float v) {
    using(AttributeType.TextureCord);
    currentVertex.textureCordinates.set(u, v);
  }
  
  public void addRectangle(float x, float y, float z, float width, float height) {
    short n1 = this.addVertex(x, y, z); // top left corner
    short n2 = this.addVertex(x, y, z+1f); // bottom left corner
    short n3 = this.addVertex(x+1f, y, z); // top right corner
    addIndices(n1,n2,n3);
    
    n1 = this.addVertex(x+1f, y, z+1f);
    addIndices(n3,n2,n1);
  }
  
  public void addIndices(short n1, short n2, short n3) {
    this.indices[indicesCursor++] = n1;
    this.indices[indicesCursor++] = n2;
    this.indices[indicesCursor++] = n3;
  }

  public boolean end() {
    if (!started) {
      return false;
    }
    started = false;
    calculateNormals();
    this.verties          = new float[vertextCount * getAttributesPerVertex()];
    boolean usingTilePos  = isUsing(AttributeType.TilePosition);
    boolean usingTextCord = isUsing(AttributeType.TextureCord);
    boolean usingNormals  = isUsing(AttributeType.Normal);
    boolean usingColor    = isUsing(AttributeType.Color);
    if (usingNormals) {
      calculateNormals();
    }
    
    vertexCursor = 0;
    for (GridVertex vertex : this.vertexsList) {
      this.verties[vertexCursor++] = vertex.position.x;
      this.verties[vertexCursor++] = vertex.position.y;
      this.verties[vertexCursor++] = vertex.position.z;
      
      if (usingNormals) {
        vertex.normal.nor();
        this.verties[vertexCursor++] = vertex.normal.x;
        this.verties[vertexCursor++] = vertex.normal.y;
        this.verties[vertexCursor++] = vertex.normal.z;
      }
      
      if (usingColor) {
        this.verties[vertexCursor++] = Color.toFloatBits(vertex.color.r, vertex.color.g, vertex.color.b, vertex.color.a);
      }
      
      if (usingTextCord) {
        this.verties[vertexCursor++] = vertex.textureCordinates.x;
        this.verties[vertexCursor++] = vertex.textureCordinates.y;
      }
      
      
      if (usingTilePos) {
        this.verties[vertexCursor++] = vertex.tilePosition.x;
        this.verties[vertexCursor++] = vertex.tilePosition.y;
      }
      
      //if (passable) {
      //  this.verties[vertexCursor++] = vertex.passable ? 1.0f : 0.0f;
      //}
    }
    
    this.mesh = null;
    return true;
  }

  public float[] getVerties() {
    return verties;
  }

  public short[] getIndices() {
    return indices;
  }

  public Mesh getMesh() {
    if (this.mesh == null) {
      this.mesh = new Mesh(true, this.verties.length, this.indices.length, this.getVertexAttributes());
      mesh.setVertices(this.verties);
      mesh.setIndices(this.indices);
      this.vertexsList.clear();
    }
    return mesh;
  }

  public VertexAttribute[] getVertexAttributes() {
    ArrayList<VertexAttribute> attributes = new ArrayList<VertexAttribute>();
    
    if (isUsing(AttributeType.Position)) {
      attributes.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
    }
    
    if (isUsing(AttributeType.Normal)) {
      attributes.add(new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));
    }
    
    if (isUsing(AttributeType.Color)) {
      attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
    }
    
    if (isUsing(AttributeType.TextureCord)) {
      attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, "a_textCords"));
    }
    
    if (isUsing(AttributeType.TilePosition)) {
      attributes.add(new VertexAttribute(Usage.Generic, 2, "a_tile_position"));
    }
    
    if (isUsing(AttributeType.Passable)) {
//      attributes.add(new VertexAttribute(Usage.Generic, 1, "a_is_passable"));
    }
    
    return attributes.toArray(new VertexAttribute[attributes.size()]);
  }

  @Override
  public void dispose() {
    if (this.mesh != null) {
      this.mesh.dispose();
    }
    this.mesh   = null;
    vertexsList = null;
  }

  public int getColumns() {
    return this.columns;
  }

  public int getRows() {
    return this.rows;
  }

  public boolean haveMeshData() {
    return this.verties != null;
  }

  
  
}
