package shipmastery.util;

public class AVLTree<T extends Comparable<T>> {
    Node<T> root;

    public void insert(T value) {
        if (root == null) {
            root = new Node<>(value, null);
        }

        insert(root, null, value);
    }

    protected Node<T> insert(Node<T> subTree, Node<T> parent, T value) {
        if (subTree == null) {
            return new Node<>(value, parent);
        }
        int compare = value.compareTo(subTree.value);
        if (compare > 0) {
            subTree.setRight(insert(subTree.right, subTree, value));
        }
        else {
            subTree.setLeft(insert(subTree.left, subTree, value));
        }
        return balance(subTree);
    }

    protected Node<T> balance(Node<T> node) {
        int l = height(node.left);
        int r = height(node.right);
        if (r - l > 1) {
            int r2 = height(node.right.right);
            int l2 = height(node.right.left);
            if (r2 <= l2) {
                node.setRight(rotateRight(node.right));
            }
            node = rotateLeft(node);
        }
        else if (l - r > 1) {
            int r2 = height(node.left.right);
            int l2 = height(node.left.left);
            if (r2 > l2) {
                node.setLeft(rotateLeft(node.left));
            }
            node = rotateRight(node);
        }
        return node;
    }

    protected Node<T> rotateLeft(Node<T> node) {
        Node<T> temp = node.right.left;
        node.height = Math.max(0, height(node.left));
        node.right.setLeft(node);
        node.right.parent = node.parent;
        node.parent = node.right;
        node.setRight(temp);
        if (temp != null) {
            temp.parent = node;
        }
        if (root == node) {
            root = node.parent;
        }
        return node.parent;
    }

    protected Node<T> rotateRight(Node<T> node) {
        Node<T> temp = node.left.right;
        node.height = Math.max(0, height(node.right));
        node.left.setRight(node);
        node.left.parent = node.parent;
        node.parent = node.left;
        node.setLeft(temp);
        if (temp != null) {
            temp.parent = node;
        }
        if (root == node) {
            root = node.parent;
        }
        return node.parent;
    }

    protected int height(Node<T> node) {
        return node == null ? -1 : node.height;
    }

    public static class Node<T extends Comparable<T>> {
        final T value;
        int height = 0;
        Node<T> left;
        Node<T> right;
        Node<T> parent;

        Node(T value, Node<T> parent) {
            this.value = value;
            this.parent = parent;
        }

        protected void setLeft(Node<T> left) {
            this.left = left;
            if (left != null) {
                height = Math.max(height, left.height + 1);
            }
        }

        protected void setRight(Node<T> right) {
            this.right = right;
            if (right != null) {
                height = Math.max(height, right.height + 1);
            }
        }
    }
}
