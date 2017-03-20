/*
 * This file is part of Go game.
 * Copyright (C) 2012   Ping Zheng [emmanuel *at* lr-studios.net]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.zhengping.gogame.Object;

import android.graphics.Point;

public class Stone {
    public Point point;
    public byte color = 0;

    public Stone(Point point, byte color) {
        this.point = point;
        this.color = color;
    }

    public Point up() {
        return new Point(point.x, point.y + 1);
    }

    public Point down() {
        return new Point(point.x, point.y - 1);
    }

    public Point left() {
        return new Point(point.x - 1, point.y);
    }

    public Point right() {
        return new Point(point.x + 1, point.y);
    }

}
