import React, {useEffect, useRef} from 'react';

// The main Tesseract animation component with optional props for size and speed
const TesseractCanvas = ({size = 100, speed = 0.0015}: { size?: number; speed?: number }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);

    // useEffect hook runs after the component mounts to setup the animation
    // It re-runs if the size or speed props change
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        let animationFrameId: number;
        let pauseTimeout = null;
        let isPaused = false;

        // --- Tesseract Geometry ---
        const points: number[][] = [];
        for (let i = 0; i < 16; i++) {
            points.push([
                (i & 1) ? 1 : -1, (i & 2) ? 1 : -1,
                (i & 4) ? 1 : -1, (i & 8) ? 1 : -1
            ]);
        }

        const faces = [
            [0, 1, 3, 2], [4, 5, 7, 6], [8, 9, 11, 10], [12, 13, 15, 14],
            [0, 1, 5, 4], [2, 3, 7, 6], [8, 9, 13, 12], [10, 11, 15, 14],
            [0, 2, 6, 4], [1, 3, 7, 5], [8, 10, 14, 12], [9, 11, 15, 13],
            [0, 1, 9, 8], [2, 3, 11, 10], [4, 5, 13, 12], [6, 7, 15, 14],
            [0, 2, 10, 8], [1, 3, 11, 9], [4, 6, 14, 12], [5, 7, 15, 13],
            [0, 4, 12, 8], [1, 5, 13, 9], [2, 6, 14, 10], [3, 7, 15, 11]
        ];

        const rainbowColors: string[] = [];
        const numColors = 3;
        for (let i = 0; i < numColors; i++) {
            const hue = 30 + (360 / numColors) * i;
            rainbowColors.push(`hsla(${hue}, 100%, 50%, 0.33)`);
        }

        // --- Rotation and Projection state ---
        let angleXY = 0;
        let angleZW = 0;
        let angleXW = 0;
        let angleYZ = 0;

        // Use the speed prop as the base speed
        const baseSpeed = speed;
        let angleXYStep = 7 * baseSpeed;
        let angleZWStep = 2 * baseSpeed;
        let angleXWStep = 3 * baseSpeed;
        let angleYZStep = 5 * baseSpeed;

        // --- Core Functions ---
        function rotate4D(p: number[]): number[] {
            let x = p[0], y = p[1], z = p[2], w = p[3];
            let tempX, tempY, tempZ, tempW;
            tempX = x * Math.cos(angleXY) - y * Math.sin(angleXY);
            tempY = x * Math.sin(angleXY) + y * Math.cos(angleXY);
            x = tempX;
            y = tempY;
            tempZ = z * Math.cos(angleZW) - w * Math.sin(angleZW);
            tempW = z * Math.sin(angleZW) + w * Math.cos(angleZW);
            z = tempZ;
            w = tempW;
            tempX = x * Math.cos(angleXW) - w * Math.sin(angleXW);
            tempW = x * Math.sin(angleXW) + w * Math.cos(angleXW);
            x = tempX;
            w = tempW;
            tempY = y * Math.cos(angleYZ) - z * Math.sin(angleYZ);
            tempZ = y * Math.sin(angleYZ) + z * Math.cos(angleYZ);
            y = tempY;
            z = tempZ;
            return [x, y, z, w];
        }

        function project4Dto2D(p_rotated: number[], scale: number, distance: number): number[] {
            const x = p_rotated[0];
            const y = p_rotated[1];
            const z = p_rotated[2];
            const w = p_rotated[3];
            const perspective_divisor = 1 / (distance - w);
            const p3d_x = x * perspective_divisor;
            const p3d_y = y * perspective_divisor;
            const p3d_z = z * perspective_divisor;
            const iso_x = (p3d_x - p3d_z);
            const iso_y = (p3d_y) + (p3d_x + p3d_z) / 2;
            return [iso_x * scale, iso_y * scale];
        }

        // --- Animation Loop ---
        const animate = () => {
            if (isPaused) {
                animationFrameId = requestAnimationFrame(animate);
                return;
            }

            angleXY += angleXYStep;
            angleZW += angleZWStep;
            angleXW += angleXWStep;
            angleYZ += angleYZStep;

            const PI_HALF = Math.PI / 2;
            const epsilon = Math.max(angleXYStep, angleZWStep, angleXWStep, angleYZStep) * 1.1;

            const isAligned =
                Math.abs(angleXY % PI_HALF) < epsilon &&
                Math.abs(angleZW % PI_HALF) < epsilon &&
                Math.abs(angleXW % PI_HALF) < epsilon &&
                Math.abs(angleYZ % PI_HALF) < epsilon;

            if (isAligned) {
                isPaused = true;
                pauseTimeout = setTimeout(() => {
                    isPaused = false;
                }, 1000); // Pause for 1 second
            }

            const rotatedPoints = points.map(p => rotate4D(p));

            const facesWithDepth = faces.map((face, index) => {
                const avg_w = (rotatedPoints[face[0]][3] + rotatedPoints[face[1]][3] + rotatedPoints[face[2]][3] + rotatedPoints[face[3]][3]) / 4;
                return {face, index, avg_w};
            });

            facesWithDepth.sort((a, b) => a.avg_w - b.avg_w);

            const scale = canvas.width / (5 / 3);
            const distance = 4;

            ctx.clearRect(0, 0, canvas.width, canvas.height);

            facesWithDepth.forEach(({face, index}) => {
                const p1 = project4Dto2D(rotatedPoints[face[0]], scale, distance);
                const p2 = project4Dto2D(rotatedPoints[face[1]], scale, distance);
                const p3 = project4Dto2D(rotatedPoints[face[2]], scale, distance);
                const p4 = project4Dto2D(rotatedPoints[face[3]], scale, distance);

                ctx.beginPath();
                ctx.moveTo(p1[0] + canvas.width / 2, p1[1] + canvas.height / 2);
                ctx.lineTo(p2[0] + canvas.width / 2, p2[1] + canvas.height / 2);
                ctx.lineTo(p3[0] + canvas.width / 2, p3[1] + canvas.height / 2);
                ctx.lineTo(p4[0] + canvas.width / 2, p4[1] + canvas.height / 2);
                ctx.closePath();

                ctx.globalCompositeOperation = 'source-over';
                ctx.strokeStyle = `rgba(255, 255, 255, 0.8)`;
                ctx.lineWidth = 1;
                ctx.lineJoin = 'round';
                ctx.stroke();

                ctx.globalCompositeOperation = 'lighter';
                ctx.fillStyle = rainbowColors[index % rainbowColors.length];
                ctx.fill();
            });

            animationFrameId = requestAnimationFrame(animate);
        };

        animate();

        return () => {
            cancelAnimationFrame(animationFrameId);
        };
    }, [size, speed]); // Dependency array includes props

    return <canvas ref={canvasRef}/>;
};

export default function Tesseract() {
    return (
        <div
            style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh',
                width: '100vw',
                backgroundColor: 'white'
            }}
        >
            <TesseractCanvas size={350} speed={0.0015} />
        </div>
    );
}
