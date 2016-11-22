function [] = makesound(amp, fs, f, duration)
values = 0:1/fs:duration;
a = amp*sin(2*pi*f*values);
sound(a);
end
