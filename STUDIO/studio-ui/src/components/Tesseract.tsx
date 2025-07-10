import React, { useEffect, useRef } from 'react'

// The main Tesseract animation component with optional props for size and speed
const TesseractCanvas = ({ size = 100, speed = 0.0015 }: { size?: number; speed?: number }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null)

    // useEffect hook runs after the component mounts to setup the animation
    // It re-runs if the size or speed props change
    useEffect(() => {
        const canvas = canvasRef.current
        if (!canvas) return

        canvas.width = size
        canvas.height = size
        const ctx = canvas.getContext('2d')
        if (!ctx) return

        let animationFrameId: number
        let isPaused = false

        // --- Tesseract Geometry ---
        const points: number[][] = []
        for (let i = 0; i < 16; i++) {
            points.push([
                (i & 1) ? 1 : -1, (i & 2) ? 1 : -1,
                (i & 4) ? 1 : -1, (i & 8) ? 1 : -1
            ])
        }

        const faces = [
            [0, 1, 3, 2], [4, 5, 7, 6], [8, 9, 11, 10], [12, 13, 15, 14],
            [0, 1, 5, 4], [2, 3, 7, 6], [8, 9, 13, 12], [10, 11, 15, 14],
            [0, 2, 6, 4], [1, 3, 7, 5], [8, 10, 14, 12], [9, 11, 15, 13],
            [0, 1, 9, 8], [2, 3, 11, 10], [4, 5, 13, 12], [6, 7, 15, 14],
            [0, 2, 10, 8], [1, 3, 11, 9], [4, 6, 14, 12], [5, 7, 15, 13],
            [0, 4, 12, 8], [1, 5, 13, 9], [2, 6, 14, 10], [3, 7, 15, 11]
        ]

        const rainbowColors: string[] = []
        const numColors = 3
        for (let i = 0; i < numColors; i++) {
            const hue = 30 + (360 / numColors) * i
            rainbowColors.push(`hsla(${hue}, 100%, 50%, 0.33)`)
        }

        // --- Rotation and Projection state ---
        let angleXY = 0
        let angleZW = 0
        let angleXW = 0
        let angleYZ = 0

        // Use the speed prop as the base speed
        let angleXYStep = 7 * speed
        let angleZWStep = 2 * speed
        let angleXWStep = 3 * speed
        let angleYZStep = 5 * speed

        // For detection when angles are equal to 90º
        const PI_HALF = Math.PI / 2
        const epsilon = Math.max(angleXYStep, angleZWStep, angleXWStep, angleYZStep) * 1.1

        // --- Core Functions ---
        function rotate4D(p: number[]): number[] {
            let x = p[0], y = p[1], z = p[2], w = p[3]
            let tempX, tempY, tempZ, tempW
            tempX = x * Math.cos(angleXY) - y * Math.sin(angleXY)
            tempY = x * Math.sin(angleXY) + y * Math.cos(angleXY)
            x = tempX
            y = tempY
            tempZ = z * Math.cos(angleZW) - w * Math.sin(angleZW)
            tempW = z * Math.sin(angleZW) + w * Math.cos(angleZW)
            z = tempZ
            w = tempW
            tempX = x * Math.cos(angleXW) - w * Math.sin(angleXW)
            tempW = x * Math.sin(angleXW) + w * Math.cos(angleXW)
            x = tempX
            w = tempW
            tempY = y * Math.cos(angleYZ) - z * Math.sin(angleYZ)
            tempZ = y * Math.sin(angleYZ) + z * Math.cos(angleYZ)
            y = tempY
            z = tempZ
            return [x, y, z, w]
        }

        function project4Dto2D(p4d: number[], distance: number, scale: number): number[] {
            const x = p4d[0]
            const y = p4d[1]
            const z = p4d[2]
            const w = p4d[3]
            // Perspective view from 4D to 3D to keep volume
            const perspective_divisor = 1 / (distance - w)
            const p3d_x = x * perspective_divisor
            const p3d_y = y * perspective_divisor
            const p3d_z = z * perspective_divisor
            // Isometric view from 3D to 2D to keep symetry
            const p2d_x = (p3d_x - p3d_z)
            const p2d_y = (p3d_y) + (p3d_x + p3d_z) / 2
            return [p2d_x * scale, p2d_y * scale]
        }

        // --- Animation Loop ---
        const animate = () => {
            if (isPaused) {
                animationFrameId = requestAnimationFrame(animate)
                return
            }

            angleXY += angleXYStep
            angleZW += angleZWStep
            angleXW += angleXWStep
            angleYZ += angleYZStep

            // Detect one of the orthogonal views
            const isAligned =
                Math.abs(angleXY % PI_HALF) < epsilon &&
                Math.abs(angleZW % PI_HALF) < epsilon &&
                Math.abs(angleXW % PI_HALF) < epsilon &&
                Math.abs(angleYZ % PI_HALF) < epsilon

            if (isAligned) {
                // Pausing on the orthogonal view
                isPaused = true
                setTimeout(() => {
                    isPaused = false
                }, 1000) // Pause for 1 second
            }

            const delta = size / 2

            const rotatedPoints = points
                .map(p => rotate4D(p))
                .map(p => project4Dto2D(p, 3, size * 0.4))

            ctx.clearRect(0, 0, size, size)

            faces.forEach((face, index) => {
                const p1 = rotatedPoints[face[0]]
                const p2 = rotatedPoints[face[1]]
                const p3 = rotatedPoints[face[2]]
                const p4 = rotatedPoints[face[3]]

                ctx.beginPath()
                ctx.moveTo(p1[0] + delta, p1[1] + delta)
                ctx.lineTo(p2[0] + delta, p2[1] + delta)
                ctx.lineTo(p3[0] + delta, p3[1] + delta)
                ctx.lineTo(p4[0] + delta, p4[1] + delta)
                ctx.closePath()

                ctx.globalCompositeOperation = 'source-over'
                ctx.strokeStyle = 'rgba(255, 255, 255, 0.8)'
                ctx.lineWidth = size / 100
                ctx.lineJoin = 'round'
                ctx.stroke()

                ctx.globalCompositeOperation = 'lighter'
                ctx.fillStyle = rainbowColors[index % rainbowColors.length]
                ctx.fill()
            })

            animationFrameId = requestAnimationFrame(animate)
        }

        animate()

        return () => {
            cancelAnimationFrame(animationFrameId)
        }
    }, [size, speed]) // Dependency array includes props

    return <canvas ref={canvasRef} />
}

export default function Tesseract({ size = 100, speed = 0.0015 }: { size?: number; speed?: number }) {
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
            <TesseractCanvas size={size} speed={speed} />
        </div>
    )
}
