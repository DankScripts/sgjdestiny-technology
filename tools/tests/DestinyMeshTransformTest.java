package com.dankscripts.sgjdestiny_dhd.client;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class DestinyMeshTransformTest {
    public static void main(String[] args) {
        Matrix4f view = new Matrix4f().rotateY((float) Math.PI / 2);
        Matrix4f model = new Matrix4f().translate(4, 3, -10).rotateY(0.3F);
        Matrix4f savedView = new Matrix4f(view);
        Matrix4f savedModel = new Matrix4f(model);
        Vector4f vertex = new Vector4f(2, 1, 0, 1);
        Vector4f expected = view.transform(model.transform(new Vector4f(vertex)));
        Vector4f actual = DestinyMeshBuffers.modelView(view, model).transform(new Vector4f(vertex));
        if (!actual.equals(expected, 0.00001F))
            throw new AssertionError("Mesh transform must apply the camera after the model");
        if (!view.equals(savedView) || !model.equals(savedModel))
            throw new AssertionError("Mesh transform must not mutate the supplied matrices");
        if (actual.equals(model.transform(new Vector4f(vertex)), 0.00001F))
            throw new AssertionError("Camera rotation must affect world-space mesh positions");
        System.out.println("PASS: cached mesh transforms include camera rotation without mutating inputs.");
    }
}
